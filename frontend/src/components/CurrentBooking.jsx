import { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useModernAlert } from './ModernAlert';
import '../customer-dashboard.css';

export default function CurrentBooking() {
  const [activeBookings, setActiveBookings] = useState([]);
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const modernAlert = useModernAlert();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [userRes, bookingsRes] = await Promise.all([
        axios.get('/api/users/me'),
        axios.get('/api/bookings/active')
      ]);
      
      setUserInfo(userRes.data);
      // Filter to show only current user's active bookings
      const myBookings = bookingsRes.data.filter(b => b.userName === user.username);
      setActiveBookings(myBookings);
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = (bookingId) => {
    modernAlert.confirmCancelBooking(async () => {
      try {
        await axios.post(`/api/bookings/${bookingId}/cancel`);
        modernAlert.success('Booking Cancelled', 'Your booking has been cancelled successfully!<br><br><strong>$6.00</strong> has been refunded to your account.');
        fetchData();
      } catch (err) {
        modernAlert.error('Cancellation Failed', err.response?.data?.message || 'Cancellation failed. Please try again.');
      }
    });
  };

  const handleReportSpotTaken = (bookingId) => {
    modernAlert.confirmReportSpotTaken(async () => {
      try {
        const res = await axios.post(`/api/bookings/${bookingId}/report-taken`);
        const updatedBooking = res.data;
        modernAlert.success('Spot Reassigned!', `Your new parking spot is <strong>${updatedBooking.parkingSpot}</strong>.<br><br>Please proceed to your new location. No additional charges applied.`);
        fetchData();
      } catch (err) {
        modernAlert.error('Report Failed', err.response?.data || 'Failed to report spot taken. Please try again.');
      }
    });
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
          <span className="navbar-brand">{userInfo?.username || 'User'}</span>
          <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarResponsive">
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="collapse navbar-collapse" id="navbarResponsive">
            <ul className="navbar-nav ml-auto">
              <li className="nav-item">
                <Link className="nav-link" to="/dashboard">User page</Link>
              </li>
              <li className="nav-item">
                <Link className="nav-link" to="/history">History</Link>
              </li>
              <li className="nav-item">
                <Link className="nav-link" to="/profile">Profile</Link>
              </li>
              <li className="nav-item active">
                <Link className="nav-link" to="/current-booking">
                  Current Booking <span className="sr-only">(current)</span>
                </Link>
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

      {/* Main Content - Current Booking Table (Matching C_userBooking.aspx) */}
      <div className="customer-table-container">
        <div className="customer-table-wrapper">
          <table className="customer-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>User ID</th>
                <th>Username</th>
                <th>Contact</th>
                <th>Credit</th>
                <th>Car Plate</th>
                <th>Parking Spot</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {activeBookings.length === 0 ? (
                <tr>
                  <td colSpan="9" className="text-center">
                    <div className="customer-empty-state">
                      <p>📅</p>
                      <p>No active bookings</p>
                      <p>You don't have any current bookings</p>
                    </div>
                  </td>
                </tr>
              ) : (
                activeBookings.map(booking => (
                  <tr key={booking.id}>
                    <td>{booking.id}</td>
                    <td>{booking.userId}</td>
                    <td>{booking.userName}</td>
                    <td>{booking.userContact}</td>
                    <td>{booking.credit || 6}</td>
                    <td>{booking.carPlate}</td>
                    <td><strong>{booking.parkingSpot}</strong></td>
                    <td>{new Date(booking.date).toLocaleString('en-US', {
                      day: '2-digit',
                      month: 'short',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}</td>
                    <td>
                      <button 
                        className="btn btn-sm btn-warning me-1" 
                        onClick={() => handleReportSpotTaken(booking.id)}
                        title="Report Spot Taken"
                      >
                        <i className="fas fa-exclamation-triangle"></i>
                      </button>
                      <button 
                        className="btn btn-sm btn-danger" 
                        onClick={() => handleCancelBooking(booking.id)}
                        title="Cancel Booking"
                      >
                        <i className="fas fa-times-circle"></i>
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
