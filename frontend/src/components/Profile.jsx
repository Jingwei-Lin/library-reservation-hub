import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { userService } from "../services/userService";
import { logoutUser } from "../services/authApi";
import { getTheme } from "../themeUtils";
import "./Profile.css";

const Profile = ({ user, setIsAuthenticated }) => {
    const navigate = useNavigate();
    const [profileData, setProfileData] = useState({
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        password: "",
        confirmPassword: ""
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [theme, setTheme] = useState(getTheme());

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                setLoading(true);
                setError("");
                setSuccess("");

                // Fetch current user profile
                const response = await userService.getUserProfile();
                if (response && response.code === 0 && response.data) {
                    setProfileData({
                        firstName: response.data.firstName,
                        lastName: response.data.lastName,
                        email: response.data.userAccount,
                        phone: response.data.phone,
                        password: "",
                        confirmPassword: ""
                    });
                } else {
                    setError(response?.message || "Failed to load profile");
                }
            } catch (err) {
                if (err.message.includes("Session expired or user banned")) {
                    logoutUser();
                    setIsAuthenticated(false);
                    navigate("/login");
                } else {
                    setError(err.message || "Failed to load profile");
                    console.error("Error fetching profile:", err);
                }
            } finally {
                setLoading(false);
            }
        };

        fetchProfile();
    }, [setIsAuthenticated, navigate]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setProfileData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (profileData.password !== profileData.confirmPassword) {
            setError("Passwords do not match!");
            return;
        }
        try {
            setError("");
            setSuccess("");

            const response = await userService.updateUserProfile(profileData);
            console.log(response);
            if (response && response.code === 0) {
                setSuccess("Profile updated successfully!");
                // Update session storage if needed
                const updatedUser = { ...user, ...profileData };
                sessionStorage.setItem("user", JSON.stringify(updatedUser));
            } else {
                setError(response?.message || "Failed to update profile.");
            }
        } catch (err) {
            setError(err.response?.data?.message || err.message || "Failed to update profile.");
            console.error("Error updating profile:", err);
        }
    };

    return (
        <div className={`profile-container ${theme === "dark" ? "dark-mode" : ""}`}>
            <h1>Profile Settings</h1>
            <button onClick={() => navigate("/dashboard")} className="back-button">
                ← Back to Dashboard
            </button>
            {loading && <div className="loading">Loading profile...</div>}
            {error && <div className="error">{error}</div>}
            {success && <div className="success">{success}</div>}
            {!loading && (
                <form onSubmit={handleSubmit} className="profile-form">
                    <div className="form-group">
                        <label htmlFor="firstName">First Name:</label>
                        <input
                            type="text"
                            id="firstName"
                            name="firstName"
                            value={profileData.firstName}
                            onChange={handleInputChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="lastName">Last Name:</label>
                        <input
                            type="text"
                            id="lastName"
                            name="lastName"
                            value={profileData.lastName}
                            onChange={handleInputChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="email">Email:</label>
                        <input
                            type="email"
                            id="email"
                            name="email"
                            value={profileData.email}
                            onChange={handleInputChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="phone">Phone:</label>
                        <input
                            type="tel"
                            id="phone"
                            name="phone"
                            value={profileData.phone}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">New Password (leave blank to keep current):</label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={profileData.password}
                            onChange={handleInputChange}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirm Password:</label>
                        <input
                            type="password"
                            id="confirmPassword"
                            name="confirmPassword"
                            value={profileData.confirmPassword}
                            onChange={handleInputChange}
                        />
                    </div>
                    <button type="submit" className="update-button">Update Profile</button>
                </form>
            )}
        </div>
    );
};

export default Profile;