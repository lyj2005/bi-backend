# bi - Intelligent BI Platform

> Author: [lyj](https://github.com/)

## Project Overview

### Project Introduction

An **Intelligent BI Platform** based on React + Spring Boot + MQ + AIGC.

Traditional projects (data analysis platforms): Analysis is performed by professional data analysts.  
My project: Users only need to input their analysis goals and upload raw data, and the system will automatically generate visual charts and insightful analysis conclusions using AI.

### Business Process

1. The client inputs analysis requirements and raw data, sending a request to the business backend.  
2. The business backend uses AI services to process the client data, stores it in the database, and generates charts.  
3. The processed data is sent from the business backend to the AI service, which generates results and returns them to the backend, ultimately displaying the results to the client.  
<img width="987" height="648" alt="image" src="https://github.com/user-attachments/assets/8285eca1-fb46-43e2-b8b1-96155d5dcea6" />

### Business Features

- User login, registration, logout, update, retrieval, and permission management  
- AI-driven chart analysis and visual presentation  

## Technology Stack

### Backend

- Java Spring Boot development framework  
- Storage layer: MySQL database + Redis cache  
- MyBatis-Plus and MyBatis X auto-generation  
- Redisson distributed lock  
- Caffeine local cache  
- ⭐️ General AI capabilities based on the deepseek large model  
- ⭐️ Redisson's built-in rate-limiting utility  
- ⭐️ Local asynchronous transformation using thread pools  
- ⭐️ Distributed asynchronous transformation using MQ  
- ⭐️ Ensuring the security of uploaded files  

### Tools

- Easy Excel for spreadsheet processing  
- Hutool utility library  
- Apache Commons Lang3 utility classes  
- Lombok annotations  
- Frontend IDE: VsCode  
- Backend IDE: JetBrains IDEA  
- [CodeGeeX Intelligent Programming Assistant](https://codegeex.cn/)