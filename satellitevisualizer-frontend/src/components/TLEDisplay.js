import './TLEDisplay.css';
import React from 'react';

// TLEDisplay component fetches and displays TLE for a satellite based on the passed in NORAD ID prop.
const TLEDisplay = ({ tle, error }) => {
    return (
        <div>
            {error && <p style={{ color: "red" }}>{error}</p>}
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
