import { useState, useEffect, useRef, useCallback } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { EventSourcePolyfill } from 'event-source-polyfill';

interface Message {
    id: string;
    sender: 'user' | 'ai';
    content: string;
    timestamp: Date;
}

const CHAT_FAILED = "Sorry, the assistant couldn't answer. Please try again.";

const useChat = () => {
    const [messages, setMessages] = useState<Message[]>([]);
    const [isTyping, setIsTyping] = useState(false);
    const [chatId] = useState<string>(uuidv4());
    const eventSourceRef = useRef<EventSource | null>(null);
    const messagesEndRef = useRef<HTMLDivElement | null>(null);

    const scrollToBottom = useCallback(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages, scrollToBottom]);

    // send a message and stream the reply in
    const sendMessage = useCallback((content: string) => {
        if (!content.trim()) return;

        const userMessage: Message = {
            id: uuidv4(),
            sender: 'user',
            content,
            timestamp: new Date(),
        };
        setMessages(prev => [...prev, userMessage]);
        setIsTyping(true);

        // an empty AI message, filled in as the reply arrives
        const aiMessageId = uuidv4();
        const aiMessage: Message = {
            id: aiMessageId,
            sender: 'ai',
            content: '',
            timestamp: new Date(),
        };
        setMessages(prev => [...prev, aiMessage]);

        // close the previous connection, if any
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        // EventSourcePolyfill instead of the browser's EventSource
        const url = `/api/ai/personal_agent/chat/server_sent_event?message=${encodeURIComponent(content)}&chatId=${chatId}`;
        const eventSource = new EventSourcePolyfill(url, {
            withCredentials: true  // send the session cookie
        });
        eventSourceRef.current = eventSource as unknown as EventSource;

        let received = false;
        eventSource.onmessage = (event: MessageEvent) => {
            received = true;
            const chunk = event.data;
            setMessages(prev =>
                prev.map(msg =>
                    msg.id === aiMessageId
                        ? { ...msg, content: msg.content + chunk }
                        : msg
                )
            );
        };

        // The server ends every reply by closing the stream, which arrives here too. Without a single chunk, the
        // request failed (a network error, the model unavailable), so the empty bubble says so.
        eventSource.onerror = () => {
            setIsTyping(false);
            eventSource.close();
            eventSourceRef.current = null;
            if (!received) {
                setMessages(prev =>
                    prev.map(msg =>
                        msg.id === aiMessageId ? { ...msg, content: CHAT_FAILED } : msg
                    )
                );
            }
        };

        return aiMessageId;
    }, [chatId]);

    // close the connection when the component unmounts
    useEffect(() => {
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);

    return {
        messages,
        isTyping,
        sendMessage,
        messagesEndRef,
        chatId
    };
};

export default useChat;
