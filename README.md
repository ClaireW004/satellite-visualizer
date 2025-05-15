# satellite-visualizer

## About
Satellite Visualizer is a web application that provides real-time satellite visualization, allowing users to track satellites using their NORAD ID and view their orbits dynamically over time. It integrates Java and Spring Boot on the backend to fetch satellite data via the [N2YO API](https://www.n2yo.com/), calculates geodetic coordinates, and stores satellite information in a MongoDB database. The frontend, built with React and CesiumJS, enables users to input satellite IDs and visualize their trajectories over time.

## Tech Stack
### Backend
- **Java** – Core logic and calculations
- **Spring Boot** – REST API implementation and data retrieval
- **MongoDB** – Database for satellite records and attributes
- **N2YO API** – Satellite TLE data source

### Frontend
- **React** – UI and component-based interaction
- **CesiumJS** – 3D visualization and orbital tracking

## How to Run:
### Setting Up MongoDB
1. Login to your MongoDB account [here](https://account.mongodb.com/account/login).
2. Look for a popup to add your current IP address, and click **Add Current IP Address**.   
3. Navigate to the MongoDBCompass application on your computer, and click the **Add New Connection** button.   
4. Enter the connection string (ask Claire for db_password and replace this, delete brackets): **mongodb+srv://TeamTech:<db_password>@cluster0.5qgsy.mongodb.net/**   
5. Click **Save** and **Connect**.   
6. You should now be able to see the satellites collection.   

### Run the Backend
1. Open IntelliJ on your computer, and click **New Project** on the Welcome screen. Navigate to the location where the satellite-visualizer was cloned, and click on **satellitevisualizer-backend** then OK.
2. Click **Run** (green triangle in top right).
3. To test out the _N2YO API_, you can enter this URL in your browser: **https://api.n2yo.com/rest/v1/satellite/tle/{noradId}&apiKey=your-api-key**.
   Replace **{noradId}** with any satellite's noradId, like 25544 for the ISS, and **your-api-key** with your own N2YO API key. If you do not have one, you can sign up for an account on the **N2YO** website. 

   To test out the _Spring Boot Endpoint_, you can enter this URL: **http://localhost:8080/api/satellite/fetch-and-save/{noradId}**. Don't forget to also replace the **{noradId}**.

### Run the Frontend
1. Open VS Code and click _File > Open Folder_ and navigate to the location where the satellite-visualizer was cloned, and click on **satellitevisualizer-frontend**.
2. Open the terminal by clicking _View > Terminal_, and run **npm start**.

## Contributing
Thank you for your interest! We welcome all improvements to this project to make it more useful to users. Check out the Contribution Guide for instructions on how to contirbute.
