# ELEC5619-Practical06-Group-4

# Library Reservation & Review Hub
# ELEC5619-Practical06-Group-4

## ⚠️ Development Notes
1. **MySQL Credentials**  
   - This project uses Spring Boot to connect to MySQL. You need to configure your local username and password for development.  
   - It is recommended to use a `.env` file or local `application.yml` to override the defaults:  
     ```yaml
     spring:
       datasource:
         username: your_mysql_username
         password: your_mysql_password
     ```  
   - **Important**: Do not commit `.env` or `application.yml` containing real credentials to GitHub. You can provide a `.env.example` as a template.  

2. **API Testing / Documentation**  
   - The project integrates Knife4j for API documentation and testing. You can open the following URL in a browser to access and test APIs:  
     [http://localhost:8432/api/doc.html#/home](http://localhost:8432/api/doc.html#/home)

---
## 1. Project Overview
**Real-world problem:**  
In many libraries, users face difficulties in **reserving books**, **tracking availability**, and **sharing reviews** with the community. Existing systems are often outdated, lack a modern interface, and fail to integrate social features.

**Our solution:**  
The **Library Reservation & Review Hub** is a web-based platform that allows library members to:
- Reserve books online and join waiting lists.
- Get real-time updates on book availability.
- Share and read community book reviews.

**High-level goals:**
- Improve user experience for library book reservations.
- Enable community engagement through reviews and ratings.
- Provide an efficient backend to handle waiting lists and availability checks.

---

## 2. Key Features
- **Book Reservation System**: Reserve books and manage pickup schedules.
- **Waiting List Management**: Automatically queue users when books are unavailable.
- **Community Review Hub**: Write and read book reviews with rating scores.
- **Real-time Availability Updates**: Instantly notify users when books are ready.
- **Search & Filter**: Find books by title, author, genre, or rating.

---

## 3. Team Roles & Responsibilities

| Name         | Role                   | Responsibilities                                             |
| ------------ | ---------------------- | ------------------------------------------------------------ |
| Hanchen Wang | Project Manager        | Oversees project timelines, coordinates communication, and ensures goals are met. |
| Hao Chen     | Backend Developer      | Implements server-side logic, API development, and system integration. |
| Huang Dayu   | Frontend Developer     | Designs and builds responsive user interfaces, integrates frontend with backend APIs. |
| Jingwei Lin  | UI/UX Designer         | Creates intuitive user experiences, wireframes, and design prototypes. |
| Yonghao Lyu  | Database Administrator | Designs database schema, manages data integrity, and optimizes queries. |
