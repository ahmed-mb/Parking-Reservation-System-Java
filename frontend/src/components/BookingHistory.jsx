import { useState, useEffect } from 'react';
import axios from 'axios';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../customer-dashboard.css';

export default function BookingHistory() {
  const [bookings, setBookings] = useState([]);
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [userRes, bookingsRes] = await Promise.all([
        axios.get('/api/users/me'),
        axios.get('/api/bookings')
      ]);
      
      setUserInfo(userRes.data);
      const userBookings = bookingsRes.data.filter(b => b.userName === user.username);
      setBookings(userBookings);
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    if (status === 'Active') {
      return <span className="badge bg-success" style={{fontSize: '12px', padding: '6px 12px'}}>ACTIVE</span>;
    } else if (status === 'Cancelled') {
      return <span className="badge bg-danger" style={{fontSize: '12px', padding: '6px 12px'}}>CANCELLED</span>;
    } else {
      return <span className="badge bg-secondary" style={{fontSize: '12px', padding: '6px 12px'}}>COMPLETED</span>;
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
          <span className="navbar-brand">{userInfo?.username || 'User'}</span>
          <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarResponsive">
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="collapse navbar-collapse" id="navbarResponsive">
            <ul className="navbar-nav ml-auto">
              <li className="nav-item">
                <Link className="nav-link" to="/dashboard">User page</Link>
              </li>
              <li className="nav-item active">
                <Link className="nav-link" to="/history">
                  History <span className="sr-only">(current)</span>
                </Link>
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

      {/* Main Content - Booking History Table (Matching C_userHistory.aspx) */}
      <div className="customer-table-container">
        <div className="customer-table-wrapper">
          <table className="customer-table">
            <thead>
              <tr>
                <th>Booking ID</th>
                <th>User ID</th>
                <th>Username</th>
                <th>Contact</th>
                <th>Credit</th>
                <th>Car Plate</th>
                <th>Parking Spot</th>
                <th>Date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {bookings.length === 0 ? (
                <tr>
                  <td colSpan="9" className="text-center">
                    <div className="customer-empty-state">
                      <p>📜</p>
                      <p>No booking history found</p>
                      <p>Booking records will appear here once you make reservations</p>
                    </div>
                  </td>
                </tr>
              ) : (
                bookings.map(booking => (
                  <tr key={booking.id}>
                    <td>{booking.id}</td>
                    <td>{booking.userId}</td>
                    <td>{booking.userName}</td>
                    <td>{booking.userContact}</td>
                    <td>{booking.credit || 6}</td>
                    <td>{booking.carPlate}</td>
                    <td>{booking.parkingSpot}</td>
                    <td>{new Date(booking.date).toLocaleString('en-US', {
                      day: '2-digit',
                      month: 'short',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}</td>
                    <td>{getStatusBadge(booking.status)}</td>
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
