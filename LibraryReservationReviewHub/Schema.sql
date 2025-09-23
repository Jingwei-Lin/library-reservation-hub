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
    available BOOLEAN DEFAULT TRUE,
    path VARCHAR(255)
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

INSERT INTO Book (book_id, title, author, genre, quantity, available, path) VALUES
(1, 'The Hobbit', 'J.R.R. Tolkien', 'Fantasy', 5, TRUE, 'books/Fantasy/The Hobbit.jpg'),
(2, 'The Lord of the Rings', 'J.R.R. Tolkien', 'Fantasy', 5, TRUE, 'books/Fantasy/The Lord of the Rings.jpg'),
(3, 'Harry Potter and the Philosopher’s Stone', 'J.K. Rowling', 'Fantasy', 5, TRUE, 'books/Fantasy/Harry Potter and the Philosopher’s Stone.jpg'),
(4, 'A Game of Thrones', 'George R.R. Martin', 'Fantasy', 5, TRUE, 'books/Fantasy/A Game of Thrones.jpg'),
(5, 'The Name of the Wind', 'Patrick Rothfuss', 'Fantasy', 5, TRUE, 'books/Fantasy/The Name of the Wind.jpg'),
(6, 'Mistborn: The Final Empire', 'Brandon Sanderson', 'Fantasy', 5, TRUE, 'books/Fantasy/Mistborn The Final Empire.jpg'),
(7, 'The Lion, the Witch and the Wardrobe', 'C.S. Lewis', 'Fantasy', 5, TRUE, 'books/Fantasy/The Lion, the Witch and the Wardrobe.jpg'),
(8, 'American Gods', 'Neil Gaiman', 'Fantasy', 5, TRUE, 'books/Fantasy/American Gods.jpg'),
(9, 'Eragon', 'Christopher Paolini', 'Fantasy', 5, TRUE, 'books/Fantasy/Eragon.jpg'),
(10, 'The Priory of the Orange Tree', 'Samantha Shannon', 'Fantasy', 5, TRUE, 'books/Fantasy/The Priory of the Orange Tree.jpg'),

(11, 'Dune', 'Frank Herbert', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Dune.jpg'),
(12, 'Neuromancer', 'William Gibson', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Neuromancer.jpg'),
(13, 'Foundation', 'Isaac Asimov', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Foundation.jpg'),
(14, 'Snow Crash', 'Neal Stephenson', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Snow Crash.jpg'),
(15, 'Ender’s Game', 'Orson Scott Card', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Ender’s Game.jpg'),
(16, 'The Martian', 'Andy Weir', 'Science Fiction', 5, TRUE, 'books/Science Fiction/The Martian.jpg'),
(17, 'Hyperion', 'Dan Simmons', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Hyperion.jpg'),
(18, 'The Left Hand of Darkness', 'Ursula K. Le Guin', 'Science Fiction', 5, TRUE, 'books/Science Fiction/The Left Hand of Darkness.jpg'),
(19, 'Ready Player One', 'Ernest Cline', 'Science Fiction', 5, TRUE, 'books/Science Fiction/Ready Player One.jpg'),
(20, 'The Three-Body Problem', 'Liu Cixin', 'Science Fiction', 5, TRUE, 'books/Science Fiction/The Three-Body Problem.jpg'),

(21, 'The Hound of the Baskervilles', 'Arthur Conan Doyle', 'Mystery', 5, TRUE, 'books/Mystery/The Hound of the Baskervilles.jpg'),
(22, 'Murder on the Orient Express', 'Agatha Christie', 'Mystery', 5, TRUE, 'books/Mystery/Murder on the Orient Express.jpg'),
(23, 'The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Mystery', 5, TRUE, 'books/Mystery/The Girl with the Dragon Tattoo.jpg'),
(24, 'Gone Girl', 'Gillian Flynn', 'Mystery', 5, TRUE, 'books/Mystery/Gone Girl.jpg'),
(25, 'Big Little Lies', 'Liane Moriarty', 'Mystery', 5, TRUE, 'books/Mystery/Big Little Lies.jpg'),
(26, 'In the Woods', 'Tana French', 'Mystery', 5, TRUE, 'books/Mystery/In the Woods.jpg'),
(27, 'The Silent Patient', 'Alex Michaelides', 'Mystery', 5, TRUE, 'books/Mystery/The Silent Patient.jpg'),
(28, 'The Da Vinci Code', 'Dan Brown', 'Mystery', 5, TRUE, 'books/Mystery/The Da Vinci Code.jpg'),
(29, 'Still Life', 'Louise Penny', 'Mystery', 5, TRUE, 'books/Mystery/Still Life.jpg'),
(30, 'The No. 1 Ladies’ Detective Agency', 'Alexander McCall Smith', 'Mystery', 5, TRUE, 'books/Mystery/The No. 1 Ladies Detective Agency.jpg'),

(31, 'Pride and Prejudice', 'Jane Austen', 'Romance', 5, TRUE, 'books/Romance/Pride and Prejudice.jpg'),
(32, 'Outlander', 'Diana Gabaldon', 'Romance', 5, TRUE, 'books/Romance/Outlander.jpg'),
(33, 'Me Before You', 'Jojo Moyes', 'Romance', 5, TRUE, 'books/Romance/Me Before You.jpg'),
(34, 'The Notebook', 'Nicholas Sparks', 'Romance', 5, TRUE, 'books/Romance/The Notebook.jpg'),
(35, 'Twilight', 'Stephenie Meyer', 'Romance', 5, TRUE, 'books/Romance/Twilight.jpg'),
(36, 'The Kiss Quotient', 'Helen Hoang', 'Romance', 5, TRUE, 'books/Romance/The Kiss Quotient.jpg'),
(37, 'It Ends with Us', 'Colleen Hoover', 'Romance', 5, TRUE, 'books/Romance/It Ends with Us.jpg'),
(38, 'Beach Read', 'Emily Henry', 'Romance', 5, TRUE, 'books/Romance/Beach Read.jpg'),
(39, 'Red, White & Royal Blue', 'Casey McQuiston', 'Romance', 5, TRUE, 'books/Romance/Red White and Royal Blue.jpg'),
(40, 'People We Meet on Vacation', 'Emily Henry', 'Romance', 5, TRUE, 'books/Romance/People We Meet on Vacation.jpg'),

(41, 'The Book Thief', 'Markus Zusak', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/The Book Thief.jpg'),
(42, 'All the Light We Cannot See', 'Anthony Doerr', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/All the Light We Cannot See.jpg'),
(43, 'The Nightingale', 'Kristin Hannah', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/The Nightingale.jpg'),
(44, 'Wolf Hall', 'Hilary Mantel', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/Wolf Hall.jpg'),
(45, 'The Tattooist of Auschwitz', 'Heather Morris', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/The Tattooist of Auschwitz.jpg'),
(46, 'A Gentleman in Moscow', 'Amor Towles', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/A Gentleman in Moscow.jpg'),
(47, 'War and Peace', 'Leo Tolstoy', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/War and Peace.jpg'),
(48, 'The Pillars of the Earth', 'Ken Follett', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/The Pillars of the Earth.jpg'),
(49, 'Beloved', 'Toni Morrison', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/Beloved.jpg'),
(50, 'The Paris Library', 'Janet Skeslien Charles', 'Historical Fiction', 5, TRUE, 'books/Historical Fiction/The Paris Library.jpg'),

(51, 'Dracula', 'Bram Stoker', 'Horror', 5, TRUE, 'books/Horror/Dracula.jpg'),
(52, 'Frankenstein', 'Mary Shelley', 'Horror', 5, TRUE, 'books/Horror/Frankenstein.jpg'),
(53, 'The Shining', 'Stephen King', 'Horror', 5, TRUE, 'books/Horror/The Shining.jpg'),
(54, 'It', 'Stephen King', 'Horror', 5, TRUE, 'books/Horror/It.jpg'),
(55, 'Mexican Gothic', 'Silvia Moreno-Garcia', 'Horror', 5, TRUE, 'books/Horror/Mexican Gothic.jpg'),
(56, 'House of Leaves', 'Mark Z. Danielewski', 'Horror', 5, TRUE, 'books/Horror/House of Leaves.jpg'),
(57, 'The Haunting of Hill House', 'Shirley Jackson', 'Horror', 5, TRUE, 'books/Horror/The Haunting of Hill House.jpg'),
(58, 'Bird Box', 'Josh Malerman', 'Horror', 5, TRUE, 'books/Horror/Bird Box.jpg'),
(59, 'Something Wicked This Way Comes', 'Ray Bradbury', 'Horror', 5, TRUE, 'books/Horror/Something Wicked This Way Comes.jpg'),
(60, 'Hell House', 'Richard Matheson', 'Horror', 5, TRUE, 'books/Horror/Hell House.jpg'),

(61, 'The Bourne Identity', 'Robert Ludlum', 'Thriller', 5, TRUE, 'books/Thriller/The Bourne Identity.jpg'),
(62, 'The Girl on the Train', 'Paula Hawkins', 'Thriller', 5, TRUE, 'books/Thriller/The Girl on the Train.jpg'),
(63, 'The Reversal', 'Michael Connelly', 'Thriller', 5, TRUE, 'books/Thriller/The Reversal.jpg'),
(64, 'Before I Go to Sleep', 'S.J. Watson', 'Thriller', 5, TRUE, 'books/Thriller/Before I Go to Sleep.jpg'),
(65, 'Shutter Island', 'Dennis Lehane', 'Thriller', 5, TRUE, 'books/Thriller/Shutter Island.jpg'),
(66, 'The Couple Next Door', 'Shari Lapena', 'Thriller', 5, TRUE, 'books/Thriller/The Couple Next Door.jpg'),
(67, 'Dark Places', 'Gillian Flynn', 'Thriller', 5, TRUE, 'books/Thriller/Dark Places.jpg'),
(68, 'I Am Watching You', 'Teresa Driscoll', 'Thriller', 5, TRUE, 'books/Thriller/I Am Watching You.jpg'),
(69, 'The Woman in the Window', 'A.J. Finn', 'Thriller', 5, TRUE, 'books/Thriller/The Woman in the Window.jpg'),
(70, 'Behind Closed Doors', 'B.A. Paris', 'Thriller', 5, TRUE, 'books/Thriller/Behind Closed Doors.jpg'),

(71, 'The Hunger Games', 'Suzanne Collins', 'Young Adult', 5, TRUE, 'books/Young Adult/The Hunger Games.jpg'),
(72, 'Divergent', 'Veronica Roth', 'Young Adult', 5, TRUE, 'books/Young Adult/Divergent.jpg'),
(73, 'The Fault in Our Stars', 'John Green', 'Young Adult', 5, TRUE, 'books/Young Adult/The Fault in Our Stars.jpg'),
(74, 'Eleanor & Park', 'Rainbow Rowell', 'Young Adult', 5, TRUE, 'books/Young Adult/Eleanor and Park.jpg'),
(75, 'Six of Crows', 'Leigh Bardugo', 'Young Adult', 5, TRUE, 'books/Young Adult/Six of Crows.jpg'),
(76, 'Throne of Glass', 'Sarah J. Maas', 'Young Adult', 5, TRUE, 'books/Young Adult/Throne of Glass.jpg'),
(77, 'A Court of Thorns and Roses', 'Sarah J. Maas', 'Young Adult', 5, TRUE, 'books/Young Adult/A Court of Thorns and Roses.jpg'),
(78, 'Legend', 'Marie Lu', 'Young Adult', 5, TRUE, 'books/Young Adult/Legend.jpg'),
(79, 'Cinder', 'Marissa Meyer', 'Young Adult', 5, TRUE, 'books/Young Adult/Cinder.jpg'),
(80, 'They Both Die at the End', 'Adam Silvera', 'Young Adult', 5, TRUE, 'books/Young Adult/They Both Die at the End.jpg'),

(81, 'Becoming', 'Michelle Obama', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/Becoming.jpg'),
(82, 'Educated', 'Tara Westover', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/Educated.jpg'),
(83, 'The Diary of a Young Girl', 'Anne Frank', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/The Diary of a Young Girl.jpg'),
(84, 'When Breath Becomes Air', 'Paul Kalanithi', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/When Breath Becomes Air.jpg'),
(85, 'Born a Crime', 'Trevor Noah', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/Born a Crime.jpg'),
(86, 'Steve Jobs', 'Walter Isaacson', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/Steve Jobs.jpg'),
(87, 'The Glass Castle', 'Jeannette Walls', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/The Glass Castle.jpg'),
(88, 'Eat, Pray, Love', 'Elizabeth Gilbert', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/Eat Pray Love.jpg'),
(89, 'Into the Wild', 'Jon Krakauer', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/Into the Wild.jpg'),
(90, 'I Am Malala', 'Malala Yousafzai', 'Non-Fiction', 5, TRUE, 'books/Non-Fiction/I Am Malala.jpg'),

(91, 'The Iliad', 'Homer', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/The Iliad.jpg'),
(92, 'The Odyssey', 'Homer', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/The Odyssey.jpg'),
(93, 'Leaves of Grass', 'Walt Whitman', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/Leaves of Grass.jpg'),
(94, 'The Waste Land', 'T.S. Eliot', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/The Waste Land.jpg'),
(95, 'Paradise Lost', 'John Milton', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/Paradise Lost.jpg'),
(96, 'The Divine Comedy', 'Dante Alighieri', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/The Divine Comedy.jpg'),
(97, 'Songs of Innocence and Experience', 'William Blake', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/Songs of Innocence and Experience.jpg'),
(98, 'The Canterbury Tales', 'Geoffrey Chaucer', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/The Canterbury Tales.jpg'),
(99, 'Ariel', 'Sylvia Plath', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/Ariel.jpg'),
(100, 'The Raven and Other Poems', 'Edgar Allan Poe', 'Poetry/Classic', 5, TRUE, 'books/Poetry Classic/The Raven and Other Poems.jpg');

INSERT INTO Waiting_list (wait_id, user_id, book_id, join_date, status) VALUES 
(1, 1, 1, CURRENT_TIMESTAMP, 'waiting');

INSERT INTO Review (review_id, user_id, book_id, rating, comment, review_date) VALUES 
(1, 1, 1, 4, 'Great book!', CURRENT_TIMESTAMP);

INSERT INTO Reservation (reservation_id, user_id, book_id, reservation_date, status) VALUES 
(1, 1, 1, CURRENT_TIMESTAMP, 'active');
