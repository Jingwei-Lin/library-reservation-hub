
-- Drop tables in correct order (to avoid FK errors)
DROP TABLE IF EXISTS Reservation CASCADE;
DROP TABLE IF EXISTS Review CASCADE;
DROP TABLE IF EXISTS Waiting_list CASCADE;
DROP TABLE IF EXISTS Users CASCADE;
DROP TABLE IF EXISTS Book CASCADE;

-- Create Users Table
CREATE TABLE Users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    phone VARCHAR(10)
);

-- Create Book Table
CREATE TABLE Book (
    book_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(50) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    quantity INT,
    available BOOLEAN
);

-- Create Waiting_list Table
CREATE TABLE Waiting_list (
    wait_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    join_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_date TIMESTAMP NULL,
    is_notified BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) CHECK (status IN ('waiting', 'notified', 'completed')) DEFAULT 'waiting',
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Create Review Table
CREATE TABLE Review (
    review_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    review_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Create Reservation Table
CREATE TABLE Reservation (
    reservation_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pickup_date TIMESTAMP NULL,
    due_date TIMESTAMP NULL,
    return_date TIMESTAMP NULL,
    status VARCHAR(20) CHECK (status IN ('active', 'completed', 'cancelled')) DEFAULT 'active',
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Insert Sample Data
INSERT INTO Users (email, password, first_name, last_name, is_admin) VALUES 
('jingwei.lin@example.com', 'pwd', 'Jingwei', 'Lin', FALSE);

INSERT INTO Book (title, author, genre, quantity, available) VALUES 
('The Great Gatsby', 'F. Scott Fitzgerald', 'Fiction', 5, TRUE);

INSERT INTO Waiting_list (user_id, book_id, join_date, status) VALUES 
(1, 1, CURRENT_TIMESTAMP, 'waiting');

INSERT INTO Review (user_id, book_id, rating, comment, review_date) VALUES 
(1, 1, 4, 'Great book!', CURRENT_TIMESTAMP);

INSERT INTO Reservation (user_id, book_id, reservation_date, status) VALUES 
(1, 1, CURRENT_TIMESTAMP, 'active');
