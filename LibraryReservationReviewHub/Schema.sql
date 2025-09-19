CREATE DATABASE IF NOT EXISTS library_reservation;
USE library_reservation;

-- Drop tables if exist
DROP TABLE IF EXISTS Reservation;
DROP TABLE IF EXISTS Review;
DROP TABLE IF EXISTS Waiting_list;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Book;

-- Create Users Table
CREATE TABLE Users (
    user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    phone VARCHAR(10)
);

-- Create Books Table
CREATE TABLE Book (
    book_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(50) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    quantity INT,
    available BOOLEAN DEFAULT TRUE
);

-- Create Waiting_List Table
CREATE TABLE Waiting_list (
    wait_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    join_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_date TIMESTAMP NULL,
    is_notified BOOLEAN DEFAULT FALSE,
    status ENUM('waiting', 'notified', 'cancelled', 'fulfilled') DEFAULT 'waiting',
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Create Reviews Table
CREATE TABLE Review (
    review_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    review_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Create Reservation Table
CREATE TABLE Reservation (
    reservation_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pickup_date TIMESTAMP NULL,
    due_date TIMESTAMP NULL,
    return_date TIMESTAMP NULL,
    status ENUM('active', 'picked_up', 'returned', 'cancelled') DEFAULT 'active',
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Insert Sample Data
INSERT INTO Users (user_id, email, password, first_name, last_name, is_admin) VALUES 
(1, 'jingwei.lin@example.com', '123456789', 'Jingwei', 'Lin', FALSE);

INSERT INTO Book (book_id, title, author, genre, quantity, available) VALUES 
(1, 'The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', 5, TRUE);

INSERT INTO Waiting_list (wait_id, user_id, book_id, join_date, status) VALUES 
(1, 1, 1, CURRENT_TIMESTAMP, 'waiting');

INSERT INTO Review (review_id, user_id, book_id, rating, comment, review_date) VALUES 
(1, 1, 1, 4, 'Great book!', CURRENT_TIMESTAMP);

INSERT INTO Reservation (reservation_id, user_id, book_id, reservation_date, status) VALUES 
(1, 1, 1, CURRENT_TIMESTAMP, 'active');
