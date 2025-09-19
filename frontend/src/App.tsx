import './App.css'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-expect-error
const Dashboard: React.FC = () => <div>Welcome to Dashboard! <button onClick={() => localStorage.removeItem('user') && (window.location.href = '/login')}>Logout</button></div>;

function App() {
    const user = localStorage.getItem('user');
    const isAuthenticated = !!user;

    return (
        <Router>
            <div className="App">
                <Routes>
                    <Route path="/login" element={!isAuthenticated ? <Login /> : <Navigate to="/dashboard" />} />
                    <Route path="/register" element={!isAuthenticated ? <Register /> : <Navigate to="/dashboard" />} />
                    <Route path="/dashboard" element={isAuthenticated ? <Dashboard /> : <Navigate to="/login" />} />
                    <Route path="/" element={<Navigate to="/login" />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;
