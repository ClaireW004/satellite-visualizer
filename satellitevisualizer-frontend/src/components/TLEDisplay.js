import './TLEDisplay.css';
import axios from 'axios';
import React, { useState } from 'react';

// TLEDisplay component fetches and displays TLE for a satellite based on the passed in NORAD ID prop.
const TLEDisplay = ({ noradId }) => {
    const [tle, setTle] = useState('');
    const [error, setError] = useState('');
    
    // Fetches TLE from backend server and is called when the Fetch TLE Data button is clicked.
    const fetchTLE = async () => {
        try {
            const response = await axios.get(`http://localhost:8080/api/satellite/${noradId}/tle`);
            console.log(response.data);
            setTle(response.data);
            setError('');
        } catch (err) {
            setError('Failed to fetch TLE data. Satellite not found or server error.');
            setTle('');
        }
    };

    // When Fetch TLE button is clicked, the TLE should be displayed in a textbox, otherwise an error will appear.
    return (
        <div>
            <button onClick={fetchTLE}>Fetch TLE Data</button>
            {error && <p style={{ color: 'red' }}>{error}</p>}
            {tle && (
                <div>
                    <h3>TLE Data:</h3>
                    <pre className="tle-box">{tle}</pre>
                </div>
            )}
        </div>
    );
};

export default TLEDisplay;
