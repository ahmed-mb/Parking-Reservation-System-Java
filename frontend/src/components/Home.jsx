import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useModernAlert } from './ModernAlert';
import Navbar from './Navbar';

export default function Home() {
  const [showModal, setShowModal] = useState(false);
  const [availableSpots, setAvailableSpots] = useState(0);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const modernAlert = useModernAlert();

  const checkAvailability = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/parking/available/count');
      setAvailableSpots(res.data.available);
      setShowModal(true);
    } catch (err) {
      console.error('Error checking availability:', err);
      modernAlert.error('Check Failed', 'Failed to check availability. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = () => {
    navigate('/login');
  };

  const closeModal = () => {
    setShowModal(false);
  };

  return (
    <>
      <Navbar />
      <div className="main">
        <div className="main-agilerow">
          <div className="signup-wthreetop">
            <h2>Welcome to Parking Reservation System</h2>
            <p>Dear user, please login in order to make the reservation, you may want to check the parking space available before logging in.</p>
          </div>
          
          <div className="contact-wthree">
            <form onSubmit={(e) => e.preventDefault()}>
              <input 
                type="submit"
                id="btnCheckAvailability"
                value={loading ? '⏳ Checking...' : 'Check Availability'}
                onClick={checkAvailability}
                disabled={loading}
              />

              <input 
                type="submit"
                id="btnLogin"
                value="Login"
                onClick={handleLogin}
              />
            </form>
          </div>
        </div>
      </div>

      {/* Availability Modal - Using Modern Alerts CSS theme */}
      {showModal && (
        <div className="modern-alert-backdrop" onClick={closeModal}>
          <div className={`modern-alert-modal ${availableSpots > 0 ? 'modal-success' : 'modal-warning'}`} onClick={(e) => e.stopPropagation()}>
            <div className="modern-alert-header">
              <h5 className="modern-alert-title">Parking Availability</h5>
            </div>
            <div className="modern-alert-body">
              <div className="modern-alert-icon">
                {availableSpots > 0 ? '\u{1F17F}\uFE0F\u2713' : '\u{1F17F}\uFE0F\u2717'}
              </div>
              <div className="modern-alert-message">
                {availableSpots > 0 ? (
                  <>
                    Great news! There are <strong>{availableSpots} parking spaces</strong> available.
                    <br /><br />
                    Please login to make your reservation.
                  </>
                ) : (
                  'All parking spots are currently occupied. Please check back later.'
                )}
              </div>
            </div>
            <div className="modern-alert-footer">
              <button 
                type="button" 
                className="btn-close-modal"
                onClick={closeModal}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
