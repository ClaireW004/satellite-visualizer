# satellite-visualizer

## How to Run:

### Setting Up MongoDB
1. Login to your MongoDB account [here](https://account.mongodb.com/account/login).
2. Look for a popup to add your current IP address, and click Add Current IP Address.
   
   ![image](https://github.com/user-attachments/assets/115d8910-2c2a-4b16-b072-d188f0c10f5c)
   
3. Navigate to the MongoDBCompass application on your computer, and click the Add New Connection button.

   ![image](https://github.com/user-attachments/assets/8210825a-1f50-475e-8fa3-61504fd94f87)
   
5. Enter the connection string (ask Claire for db_password and replace, delete brackets): mongodb+srv://TeamTech:<db_password>@cluster0.5qgsy.mongodb.net/

   ![image](https://github.com/user-attachments/assets/6729de1c-ad6d-48c2-b5ba-815e7a40b098)
   
7. Click Save and Connect.

   ![image](https://github.com/user-attachments/assets/778840bd-7df4-40c3-96ce-1f3e6fbed872)
   
9. You should now be able to see the satellites collection.

   ![image](https://github.com/user-attachments/assets/fc2d6ad5-9499-44e8-a399-61305872ec4a)
   

### Run the Backend
7. Open IntelliJ on your computer, and click "New Project" on the Welcome screen. Navigate to the location where the satellite-visualizer was cloned, and click on **satellitevisualizer-backend** then OK.

   ![image](https://github.com/user-attachments/assets/4915f6c8-9e5f-4076-9faa-8bd7dad16395)
   
8. Click Run (green triangle in top right).
9. To test out the N2YO API, you can enter this URL in your browser: **https://api.n2yo.com/rest/v1/satellite/tle/{noradId}&apiKey=your-api-key**. Replace **{noradId}** with any satellite's noradId, like 25544 for the ISS, and **your-api-key** with your own N2YO API key. If you do not have one, you can sign up for an account on the N2YO website. The result should look something like this:

   ![image](https://github.com/user-attachments/assets/81c3dc5e-29a7-4a2f-a9c7-09aeefce7034)

   To test out the Spring Boot Endpoint, you can enter this URL: **http://localhost:8080/api/satellite/fetch-and-save/{noradId}**. Don't forget to also replace the **{noradId}**.

   ![image](https://github.com/user-attachments/assets/2a7d9d78-748b-4201-bedd-e94b90b55077)
   
11. If you used the second URL, you can go back to MongoDBCompass and you should see that satellite data saved as a document.

    ![image](https://github.com/user-attachments/assets/ced2136d-e22d-42a7-af16-9a9ed4bd25f9)


### Run the Frontend
11. Open VS Code and click File > Open Folder and navigate to the location where the satellite-visualizer was cloned, and click on **satellitevisualizer-frontend**.
12. Open the terminal by clicking View > Terminal, and run **npm start**.
