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
	phone VARCHAR(10),
	status INT DEFAULT 1 COMMENT '1 means active, 0 means inactive',
	create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
	update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


-- Create Books Table
CREATE TABLE Book (
	book_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	title VARCHAR(255) NOT NULL,
	author VARCHAR(50) NOT NULL,
	genre VARCHAR(50) NOT NULL,
	quantity INT,
	available BOOLEAN DEFAULT TRUE,
	`path` VARCHAR(255)
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
	 reservation_date TIMESTAMP NULL,
	 pickup_date TIMESTAMP NULL,
	 due_date TIMESTAMP NULL,
	 return_date TIMESTAMP NULL,
	 status ENUM('reserved', 'borrowed', 'returned', 'cancelled') DEFAULT 'reserved',
	 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	 update_time TIMESTAMP NULL,
	 is_delete BOOLEAN DEFAULT FALSE,
	 FOREIGN KEY (user_id) REFERENCES Users(user_id),
	 FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

-- Insert Sample Data
INSERT INTO Users (user_id, email, password, first_name, last_name, is_admin) VALUES
    (1, 'jingwei.lin2@example.com', '0fcfcbbb420d3b6f81cd5a0d70a96579', 'Jingwei', 'Lin', FALSE),
    (2, 'hanchen.wang@example.com', '0fcfcbbb420d3b6f81cd5a0d70a96579', 'Hanchen', 'Wang', FALSE);


INSERT INTO Book (book_id, title, author, genre, quantity, available, `path`) VALUES
	(1, 'The Hobbit', 'J.R.R. Tolkien', 'Fantasy', 5, TRUE, 'The_Hobbit.jpg'),
	(2, 'The Lord of the Rings', 'J.R.R. Tolkien', 'Fantasy', 5, TRUE, 'The_Lord_of_the_Rings.jpg'),
	(3, 'Harry Potter and the Philosopher’s Stone', 'J.K. Rowling', 'Fantasy', 5, TRUE, 'Harry_Potter_and_the_Philosophers_Stone.jpg'),
	(4, 'A Game of Thrones', 'George R.R. Martin', 'Fantasy', 5, TRUE, 'A_Game_of_Thrones.jpg'),
	(5, 'The Name of the Wind', 'Patrick Rothfuss', 'Fantasy', 5, TRUE, 'The_Name_of_the_Wind.jpg'),
	(6, 'Mistborn: The Final Empire', 'Brandon Sanderson', 'Fantasy', 5, TRUE, 'Mistborn_The_Final_Empire.jpg'),
	(7, 'The Lion, the Witch and the Wardrobe', 'C.S. Lewis', 'Fantasy', 5, TRUE, 'The_Lion_the_Witch_and_the_Wardrobe.jpg'),
	(8, 'American Gods', 'Neil Gaiman', 'Fantasy', 5, TRUE, 'American_Gods.jpg'),
	(9, 'Eragon', 'Christopher Paolini', 'Fantasy', 5, TRUE, 'Eragon.jpg'),
	(10, 'The Priory of the Orange Tree', 'Samantha Shannon', 'Fantasy', 5, TRUE, 'The_Priory_of_the_Orange_Tree.jpg'),

	(11, 'Dune', 'Frank Herbert', 'Science Fiction', 5, TRUE, 'Dune.jpg'),
	(12, 'Neuromancer', 'William Gibson', 'Science Fiction', 5, TRUE, 'Neuromancer.jpg'),
	(13, 'Foundation', 'Isaac Asimov', 'Science Fiction', 5, TRUE, 'Foundation.jpg'),
	(14, 'Snow Crash', 'Neal Stephenson', 'Science Fiction', 5, TRUE, 'Snow_Crash.jpg'),
	(15, 'Ender’s Game', 'Orson Scott Card', 'Science Fiction', 5, TRUE, 'Enders_Game.jpg'),
	(16, 'The Martian', 'Andy Weir', 'Science Fiction', 5, TRUE, 'The_Martian.jpg'),
	(17, 'Hyperion', 'Dan Simmons', 'Science Fiction', 5, TRUE, 'Hyperion.jpg'),
	(18, 'The Left Hand of Darkness', 'Ursula K. Le Guin', 'Science Fiction', 5, TRUE, 'The_Left_Hand_of_Darkness.jpg'),
	(19, 'Ready Player One', 'Ernest Cline', 'Science Fiction', 5, TRUE, 'Ready_Player_One.jpg'),
	(20, 'The Three-Body Problem', 'Liu Cixin', 'Science Fiction', 5, TRUE, 'The_Three_Body_Problem.jpg'),

	(21, 'The Hound of the Baskervilles', 'Arthur Conan Doyle', 'Mystery', 5, TRUE, 'The_Hound_of_the_Baskervilles.jpg'),
	(22, 'Murder on the Orient Express', 'Agatha Christie', 'Mystery', 5, TRUE, 'Murder_on_the_Orient_Express.jpg'),
	(23, 'The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Mystery', 5, TRUE, 'The_Girl_with_the_Dragon_Tattoo.jpg'),
	(24, 'Gone Girl', 'Gillian Flynn', 'Mystery', 5, TRUE, 'Gone_Girl.jpg'),
	(25, 'Big Little Lies', 'Liane Moriarty', 'Mystery', 5, TRUE, 'Big_Little_Lies.jpg'),
	(26, 'In the Woods', 'Tana French', 'Mystery', 5, TRUE, 'In_the_Woods.jpg'),
	(27, 'The Silent Patient', 'Alex Michaelides', 'Mystery', 5, TRUE, 'The_Silent_Patient.jpg'),
	(28, 'The Da Vinci Code', 'Dan Brown', 'Mystery', 5, TRUE, 'The_Da_Vinci_Code.jpg'),
	(29, 'Still Life', 'Louise Penny', 'Mystery', 5, TRUE, 'Still_Life.jpg'),
	(30, 'The No. 1 Ladies’ Detective Agency', 'Alexander McCall Smith', 'Mystery', 5, TRUE, 'The_No_1_Ladies_Detective_Agency.jpg'),

	(31, 'Pride and Prejudice', 'Jane Austen', 'Romance', 5, TRUE, 'Pride_and_Prejudice.jpg'),
	(32, 'Outlander', 'Diana Gabaldon', 'Romance', 5, TRUE, 'Outlander.jpg'),
	(33, 'Me Before You', 'Jojo Moyes', 'Romance', 5, TRUE, 'Me_Before_You.jpg'),
	(34, 'The Notebook', 'Nicholas Sparks', 'Romance', 5, TRUE, 'The_Notebook.jpg'),
	(35, 'Twilight', 'Stephenie Meyer', 'Romance', 5, TRUE, 'Twilight.jpg'),
	(36, 'The Kiss Quotient', 'Helen Hoang', 'Romance', 5, TRUE, 'The_Kiss_Quotient.jpg'),
	(37, 'It Ends with Us', 'Colleen Hoover', 'Romance', 5, TRUE, 'It_Ends_with_Us.jpg'),
	(38, 'Beach Read', 'Emily Henry', 'Romance', 5, TRUE, 'Beach_Read.jpg'),
	(39, 'Red, White & Royal Blue', 'Casey McQuiston', 'Romance', 5, TRUE, 'Red_White_and_Royal_Blue.jpg'),
	(40, 'People We Meet on Vacation', 'Emily Henry', 'Romance', 5, TRUE, 'People_We_Meet_on_Vacation.jpg'),

	(41, 'The Book Thief', 'Markus Zusak', 'Historical Fiction', 5, TRUE, 'The_Book_Thief.jpg'),
	(42, 'All the Light We Cannot See', 'Anthony Doerr', 'Historical Fiction', 5, TRUE, 'All_the_Light_We_Cannot_See.jpg'),
	(43, 'The Nightingale', 'Kristin Hannah', 'Historical Fiction', 5, TRUE, 'The_Nightingale.jpg'),
	(44, 'Wolf Hall', 'Hilary Mantel', 'Historical Fiction', 5, TRUE, 'Wolf_Hall.jpg'),
	(45, 'The Tattooist of Auschwitz', 'Heather Morris', 'Historical Fiction', 5, TRUE, 'The_Tattooist_of_Auschwitz.jpg'),
	(46, 'A Gentleman in Moscow', 'Amor Towles', 'Historical Fiction', 5, TRUE, 'A_Gentleman_in_Moscow.jpg'),
	(47, 'War and Peace', 'Leo Tolstoy', 'Historical Fiction', 5, TRUE, 'War_and_Peace.jpg'),
	(48, 'The Pillars of the Earth', 'Ken Follett', 'Historical Fiction', 5, TRUE, 'The_Pillars_of_the_Earth.jpg'),
	(49, 'Beloved', 'Toni Morrison', 'Historical Fiction', 5, TRUE, 'Beloved.jpg'),
	(50, 'The Paris Library', 'Janet Skeslien Charles', 'Historical Fiction', 5, TRUE, 'The_Paris_Library.jpg'),

	(51, 'Dracula', 'Bram Stoker', 'Horror', 5, TRUE, 'Dracula.jpg'),
	(52, 'Frankenstein', 'Mary Shelley', 'Horror', 5, TRUE, 'Frankenstein.jpg'),
	(53, 'The Shining', 'Stephen King', 'Horror', 5, TRUE, 'The_Shining.jpg'),
	(54, 'It', 'Stephen King', 'Horror', 5, TRUE, 'It.jpg'),
	(55, 'Mexican Gothic', 'Silvia Moreno-Garcia', 'Horror', 5, TRUE, 'Mexican_Gothic.jpg'),
	(56, 'House of Leaves', 'Mark Z. Danielewski', 'Horror', 5, TRUE, 'House_of_Leaves.jpg'),
	(57, 'The Haunting of Hill House', 'Shirley Jackson', 'Horror', 5, TRUE, 'The_Haunting_of_Hill_House.jpg'),
	(58, 'Bird Box', 'Josh Malerman', 'Horror', 5, TRUE, 'Bird_Box.jpg'),
	(59, 'Something Wicked This Way Comes', 'Ray Bradbury', 'Horror', 5, TRUE, 'Something_Wicked_This_Way_Comes.jpg'),
	(60, 'Hell House', 'Richard Matheson', 'Horror', 5, TRUE, 'Hell_House.jpg'),

	(61, 'The Bourne Identity', 'Robert Ludlum', 'Thriller', 5, TRUE, 'The_Bourne_Identity.jpg'),
	(62, 'The Girl on the Train', 'Paula Hawkins', 'Thriller', 5, TRUE, 'The_Girl_on_the_Train.jpg'),
	(63, 'The Reversal', 'Michael Connelly', 'Thriller', 5, TRUE, 'The_Reversal.jpg'),
	(64, 'Before I Go to Sleep', 'S.J. Watson', 'Thriller', 5, TRUE, 'Before_I_Go_to_Sleep.jpg'),
	(65, 'Shutter Island', 'Dennis Lehane', 'Thriller', 5, TRUE, 'Shutter_Island.jpg'),
	(66, 'The Couple Next Door', 'Shari Lapena', 'Thriller', 5, TRUE, 'The_Couple_Next_Door.jpg'),
	(67, 'Dark Places', 'Gillian Flynn', 'Thriller', 5, TRUE, 'Dark_Places.jpg'),
	(68, 'I Am Watching You', 'Teresa Driscoll', 'Thriller', 5, TRUE, 'I_Am_Watching_You.jpg'),
	(69, 'The Woman in the Window', 'A.J. Finn', 'Thriller', 5, TRUE, 'The_Woman_in_the_Window.jpg'),
	(70, 'Behind Closed Doors', 'B.A. Paris', 'Thriller', 5, TRUE, 'Behind_Closed_Doors.jpg'),

	(71, 'The Hunger Games', 'Suzanne Collins', 'Young Adult', 5, TRUE, 'The_Hunger_Games.jpg'),
	(72, 'Divergent', 'Veronica Roth', 'Young Adult', 5, TRUE, 'Divergent.jpg'),
	(73, 'The Fault in Our Stars', 'John Green', 'Young Adult', 5, TRUE, 'The_Fault_in_Our_Stars.jpg'),
	(74, 'Eleanor & Park', 'Rainbow Rowell', 'Young Adult', 5, TRUE, 'Eleanor_and_Park.jpg'),
	(75, 'Six of Crows', 'Leigh Bardugo', 'Young Adult', 5, TRUE, 'Six_of_Crows.jpg'),
	(76, 'Throne of Glass', 'Sarah J. Maas', 'Young Adult', 5, TRUE, 'Throne_of_Glass.jpg'),
	(77, 'A Court of Thorns and Roses', 'Sarah J. Maas', 'Young Adult', 5, TRUE, 'A_Court_of_Thorns_and_Roses.jpg'),
	(78, 'Legend', 'Marie Lu', 'Young Adult', 5, TRUE, 'Legend.jpg'),
	(79, 'Cinder', 'Marissa Meyer', 'Young Adult', 5, TRUE, 'Cinder.jpg'),
	(80, 'They Both Die at the End', 'Adam Silvera', 'Young Adult', 5, TRUE, 'They_Both_Die_at_the_End.jpg'),

	(81, 'Becoming', 'Michelle Obama', 'Non-Fiction', 5, TRUE, 'Becoming.jpg'),
	(82, 'Educated', 'Tara Westover', 'Non-Fiction', 5, TRUE, 'Educated.jpg'),
	(83, 'The Diary of a Young Girl', 'Anne Frank', 'Non-Fiction', 5, TRUE, 'The_Diary_of_a_Young_Girl.jpg'),
	(84, 'When Breath Becomes Air', 'Paul Kalanithi', 'Non-Fiction', 5, TRUE, 'When_Breath_Becomes_Air.jpg'),
	(85, 'Born a Crime', 'Trevor Noah', 'Non-Fiction', 5, TRUE, 'Born_a_Crime.jpg'),
	(86, 'Steve Jobs', 'Walter Isaacson', 'Non-Fiction', 5, TRUE, 'Steve_Jobs.jpg'),
	(87, 'The Glass Castle', 'Jeannette Walls', 'Non-Fiction', 5, TRUE, 'The_Glass_Castle.jpg'),
	(88, 'Eat, Pray, Love', 'Elizabeth Gilbert', 'Non-Fiction', 5, TRUE, 'Eat_Pray_Love.jpg'),
	(89, 'Into the Wild', 'Jon Krakauer', 'Non-Fiction', 5, TRUE, 'Into_the_Wild.jpg'),
	(90, 'I Am Malala', 'Malala Yousafzai', 'Non-Fiction', 5, TRUE, 'I_Am_Malala.jpg'),

	(91, 'The Iliad', 'Homer', 'Poetry Classic', 5, TRUE, 'The_Iliad.jpg'),
	(92, 'The Odyssey', 'Homer', 'Poetry Classic', 5, TRUE, 'The_Odyssey.jpg'),
	(93, 'Leaves of Grass', 'Walt Whitman', 'Poetry Classic', 5, TRUE, 'Leaves_of_Grass.jpg'),
	(94, 'The Waste Land', 'T.S. Eliot', 'Poetry Classic', 5, TRUE, 'The_Waste_Land.jpg'),
	(95, 'Paradise Lost', 'John Milton', 'Poetry Classic', 5, TRUE, 'Paradise_Lost.jpg'),
	(96, 'The Divine Comedy', 'Dante Alighieri', 'Poetry Classic', 5, TRUE, 'The_Divine_Comedy.jpg'),
	(97, 'Songs of Innocence and Experience', 'William Blake', 'Poetry Classic', 5, TRUE, 'Songs_of_Innocence_and_Experience.jpg'),
	(98, 'The Canterbury Tales', 'Geoffrey Chaucer', 'Poetry Classic', 5, TRUE, 'The_Canterbury_Tales.jpg'),
	(99, 'Ariel', 'Sylvia Plath', 'Poetry Classic', 5, TRUE, 'Ariel.jpg'),
	(100, 'The Raven and Other Poems', 'Edgar Allan Poe', 'Poetry Classic', 5, TRUE, 'The_Raven_and_Other_Poems.jpg');

INSERT INTO Waiting_list (wait_id, user_id, book_id, join_date, status) VALUES
    (1, 1, 1, CURRENT_TIMESTAMP, 'waiting');

INSERT INTO Review (review_id, user_id, book_id, rating, comment, review_date) VALUES
   (1, 1, 1, 4, 'Great book!', CURRENT_TIMESTAMP),
   (2, 1, 1, 5, 'An epic adventure with rich world-building!', CURRENT_TIMESTAMP),
   (3, 2, 1, 3, 'Good story, but a bit slow in the middle.', CURRENT_TIMESTAMP),
   (4, 1, 2, 4, 'A classic fantasy masterpiece.', CURRENT_TIMESTAMP),
   (5, 2, 2, 5, 'Loved the depth of the characters and plot!', CURRENT_TIMESTAMP),
   (6, 1, 3, 4, 'Magical and engaging, great for all ages.', CURRENT_TIMESTAMP),
   (7, 2, 4, 2, 'Too complex for my taste.', CURRENT_TIMESTAMP),
   (8, 1, 5, 5, 'Kvothe’s story is captivating!', CURRENT_TIMESTAMP);

INSERT INTO Reservation (reservation_id, user_id, book_id, reservation_date, status) VALUES
    (1, 1, 1, CURRENT_TIMESTAMP, 'active');
