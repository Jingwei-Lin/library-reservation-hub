import { useEffect, useState } from "react";
import { getTheme } from "../themeUtils";

/**
 * The colour theme ("light" or "dark"), kept on <body> as a class. It follows the toggle on this page (the
 * themeChange event) and in other tabs (the storage event).
 */
export const useTheme = () => {
    const [theme, setTheme] = useState(getTheme());

    // Apply theme from localStorage
    useEffect(() => {
        document.body.classList.remove("light", "dark");
        document.body.classList.add(theme);
    }, [theme]);

    // Listen for custom themeChange event (in-page sync)
    useEffect(() => {
        const handleThemeChange = (event) => {
            setTheme(event.detail.theme);
        };
        window.addEventListener("themeChange", handleThemeChange);
        return () => {
            window.removeEventListener("themeChange", handleThemeChange);
        };
    }, []);

    // Poll localStorage for theme changes (cross-tab sync)
    useEffect(() => {
        const handleStorageChange = () => {
            setTheme(getTheme());
        };
        window.addEventListener("storage", handleStorageChange);
        return () => {
            window.removeEventListener("storage", handleStorageChange);
        };
    }, []);

    return theme;
};
