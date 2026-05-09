import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useModernAlert } from './ModernAlert';
import '../customer-dashboard.css';

export default function Dashboard() {
  const [userInfo, setUserInfo] = useState(null);
  const [activeBookings, setActiveBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);
  // Only `logout` is needed for the navbar; `user` was a dead destructure.
  const { logout } = useAuth();
  const navigate = useNavigate();
  const modernAlert = useModernAlert();

  const fetchData = useCallback(async () => {
    try {
      const [userRes, bookingsRes] = await Promise.all([
        axios.get('/api/users/me'),
        axios.get('/api/bookings/my-active'),
      ]);
      setUserInfo(userRes.data);
      setActiveBookings(bookingsRes.data);
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleBookNow = async () => {
    if (!userInfo) return;
    
    // Check if user has enough credit
    if (userInfo.credit < 6) {
      modernAlert.error('Insufficient Credit', 'You need at least <strong>$6.00</strong> to book a parking spot.<br><br>Please top up your account to continue.');
      return;
    }

    // Check if user already has an active booking
    if (activeBookings.length > 0) {
      modernAlert.warning('Active Booking Exists', 'You already have an active booking.<br><br>Please cancel it before booking another spot.');
      navigate('/current-booking');
      return;
    }

    setBookingLoading(true);
    try {
      // Get available parking spots
      const parkingRes = await axios.get('/api/parking/available');
      
      if (parkingRes.data.length === 0) {
        modernAlert.error('No Spots Available', 'Sorry, no parking spots are currently available.<br><br>Please try again later.');
        setBookingLoading(false);
        return;
      }

      // Book the first available spot
      const firstAvailableSpot = parkingRes.data[0];
      
      await axios.post('/api/bookings', {
        userId: userInfo.id,
        parkingId: firstAvailableSpot.parkingId,
        carPlate: userInfo.carPlateNo
      });
      
      modernAlert.success('Booking Successful!', `You have been assigned parking spot <strong>${firstAvailableSpot.parkingId}</strong>.<br><br><strong>$6.00</strong> has been deducted from your account.`);
      navigate('/current-booking');
    } catch (err) {
      modernAlert.error('Booking Failed', err.response?.data?.message || 'Booking failed. Please try again.');
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <div className="spinner-border text-light" role="status" />
      </div>
    );
  }

  return (
    <div style={{minHeight: '100vh'}}>
      {/* Navigation - Matching ASP.NET customer navbar */}
      <nav className="navbar navbar-expand-lg navbar-dark fixed-top customer-navbar">
        <div className="container">
          <span className="navbar-brand">Welcome {userInfo?.username || 'User'}</span>
          <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarResponsive">
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="collapse navbar-collapse" id="navbarResponsive">
            <ul className="navbar-nav ml-auto">
              <li className="nav-item active">
                <Link className="nav-link" to="/dashboard">
                  User page <span className="sr-only">(current)</span>
                </Link>
              </li>
              <li className="nav-item">
                <Link className="nav-link" to="/history">History</Link>
              </li>
              <li className="nav-item">
                <Link className="nav-link" to="/profile">Profile</Link>
              </li>
              <li className="nav-item">
                <Link className="nav-link" to="/current-booking">Current Booking</Link>
              </li>
              <li className="nav-item">
                <a className="nav-link" href="#" onClick={(e) => { e.preventDefault(); logout(); navigate('/login'); }}>
                  Logout
                </a>
              </li>
            </ul>
          </div>
        </div>
      </nav>

      {/* Main Content - Simple Book Now Button (Matching C_userPage.aspx) */}
      <div className="booking-container">
        <div className="booking-card">
          <h2>Reserve Your Parking Spot</h2>
          <p>Book your parking space with just one click! Quick, easy, and secure parking reservation system.</p>
          <button 
            className="btn-book-now" 
            onClick={handleBookNow}
            disabled={bookingLoading}
          >
            {bookingLoading ? 'BOOKING...' : 'BOOK NOW'}
          </button>
        </div>
      </div>
    </div>
  );
}
