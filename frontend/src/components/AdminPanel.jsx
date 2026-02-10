import { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useModernAlert } from './ModernAlert';
import '../admin-panel.css';

export default function AdminPanel() {
  const [activeView, setActiveView] = useState('bookings'); // Default to bookings (matching ASP.NET)
  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [parkingSpots, setParkingSpots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingUserId, setEditingUserId] = useState(null);
  const [editFormData, setEditFormData] = useState({});
  const { logout } = useAuth();
  const navigate = useNavigate();
  const modernAlert = useModernAlert();

  useEffect(() => {
    fetchData();
  }, [activeView]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeView === 'users') {
        const res = await axios.get('/api/admin/users');
        setUsers(res.data);
      } else if (activeView === 'bookings') {
        const res = await axios.get('/api/admin/bookings');
        setBookings(res.data);
      } else if (activeView === 'parking') {
        const res = await axios.get('/api/admin/parking');
        setParkingSpots(res.data);
      }
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEditUser = (user) => {
    setEditingUserId(user.id);
    setEditFormData({
      username: user.username,
      email: user.email,
      mobile: user.mobile,
      address: user.address,
      carPlateNo: user.carPlateNo,
      credit: user.credit
    });
  };

  const handleCancelEdit = () => {
    setEditingUserId(null);
    setEditFormData({});
  };

  const handleUpdateUser = async (userId) => {
    try {
      await axios.put(`/api/admin/users/${userId}`, editFormData);
      modernAlert.success('User Updated', 'Customer information has been updated successfully.');
      setEditingUserId(null);
      setEditFormData({});
      fetchData();
    } catch (err) {
      modernAlert.error('Update Failed', 'Failed to update user: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDeleteUser = (userId) => {
    modernAlert.confirmDeleteCustomer(async () => {
      try {
        await axios.delete(`/api/admin/users/${userId}`);
        modernAlert.success('Customer Deleted', 'The customer has been deleted successfully.');
        fetchData();
      } catch (err) {
        modernAlert.error('Delete Failed', 'Failed to delete customer: ' + (err.response?.data?.message || err.message));
      }
    });
  };

  const handleDeleteBooking = (bookingId) => {
    modernAlert.confirmDeleteBooking(async () => {
      try {
        await axios.delete(`/api/admin/bookings/${bookingId}`);
        modernAlert.success('Booking Deleted', 'The booking has been deleted successfully.<br><br>The parking spot has been released and credits have been refunded.');
        fetchData();
      } catch (err) {
        modernAlert.error('Delete Failed', 'Failed to delete booking: ' + (err.response?.data?.message || err.message));
      }
    });
  };

  const getStatusBadge = (availability) => {
    if (availability === 'available') {
      return <span className="badge bg-success" style={{fontSize: '14px', padding: '8px 16px'}}>AVAILABLE</span>;
    } else if (availability === 'occupied' || availability === 'booked') {
      return <span className="badge bg-danger" style={{fontSize: '14px', padding: '8px 16px'}}>OCCUPIED</span>;
    } else {
      return <span className="badge bg-warning" style={{fontSize: '14px', padding: '8px 16px'}}>{availability?.toUpperCase()}</span>;
    }
  };

  return (
    <div style={{minHeight: '100vh'}}>
      {/* Navigation - Matching ASP.NET navbar exactly */}
      <nav className="navbar navbar-expand-lg navbar-dark fixed-top admin-navbar">
        <div className="container">
          <a className="navbar-brand" href="#" onClick={(e) => { e.preventDefault(); setActiveView('bookings'); }}>
            ADMIN HOME
          </a>
          <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarResponsive">
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="collapse navbar-collapse" id="navbarResponsive">
            <ul className="navbar-nav ml-auto">
              <li className={`nav-item ${activeView === 'bookings' ? 'active' : ''}`}>
                <a className="nav-link" href="#" onClick={(e) => { e.preventDefault(); setActiveView('bookings'); }}>
                  View Booking History {activeView === 'bookings' && <span className="sr-only">(current)</span>}
                </a>
              </li>
              <li className={`nav-item ${activeView === 'users' ? 'active' : ''}`}>
                <a className="nav-link" href="#" onClick={(e) => { e.preventDefault(); setActiveView('users'); }}>
                  View Customers
                </a>
              </li>
              <li className={`nav-item ${activeView === 'parking' ? 'active' : ''}`}>
                <a className="nav-link" href="#" onClick={(e) => { e.preventDefault(); setActiveView('parking'); }}>
                  View Parking
                </a>
              </li>
              <li className="nav-item">
                <a className="nav-link" href="#" onClick={(e) => { e.preventDefault(); logout(); navigate('/login'); }}>
                  Log-out
                </a>
              </li>
            </ul>
          </div>
        </div>
      </nav>

      {/* Main Content - Matching ASP.NET table-container style */}
      <div className="admin-table-container">
        {loading ? (
          <div className="text-center"><div className="spinner-border text-light" /></div>
        ) : (
          <>
            {/* View Booking History */}
            {activeView === 'bookings' && (
              <div className="admin-table-wrapper">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>Booking ID</th>
                      <th>User ID</th>
                      <th>Username</th>
                      <th>Contact</th>
                      <th>Credit</th>
                      <th>Car Plate</th>
                      <th>Parking Spot</th>
                      <th>Status</th>
                      <th>Date</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bookings.length === 0 ? (
                      <tr>
                        <td colSpan="10" className="text-center">
                          <div className="admin-empty-state">
                            <p>📋</p>
                            <p>No booking history found</p>
                            <p>Booking records will appear here once customers make reservations</p>
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
                          <td>{booking.credit || 0}</td>
                          <td>{booking.carPlate}</td>
                          <td>{booking.parkingSpot}</td>
                          <td>
                            <span className={`badge ${
                              booking.status === 'Active' ? 'bg-success' :
                              booking.status === 'Cancelled' ? 'bg-danger' : 'bg-secondary'
                            }`} style={{fontSize: '12px'}}>
                              {booking.status}
                            </span>
                          </td>
                          <td>{new Date(booking.date).toLocaleString('en-US', {
                            day: '2-digit',
                            month: 'short',
                            year: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                          })}</td>
                          <td>
                            {booking.status === 'Active' ? (
                              <button 
                                className="btn btn-sm btn-danger" 
                                onClick={() => handleDeleteBooking(booking.id)}
                                title="Delete Booking"
                              >
                                <i className="fas fa-trash-alt"></i>
                              </button>
                            ) : (
                              <span 
                                className="btn btn-sm btn-secondary disabled" 
                                style={{opacity: 0.3, cursor: 'not-allowed'}}
                                title="Cannot delete completed/cancelled bookings"
                              >
                                <i className="fas fa-ban"></i>
                              </span>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}

            {/* View Customers */}
            {activeView === 'users' && (
              <div className="admin-table-wrapper">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Username</th>
                      <th>Email</th>
                      <th>Mobile</th>
                      <th>Address</th>
                      <th>Car Plate</th>
                      <th>Credit</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="text-center">
                          <div className="admin-empty-state">
                            <p>👥</p>
                            <p>No customer records found</p>
                            <p>Customer records will appear here once users register</p>
                          </div>
                        </td>
                      </tr>
                    ) : (
                      users.map(user => (
                        <tr key={user.id}>
                          {editingUserId === user.id ? (
                            <>
                              <td>{user.id}</td>
                              <td>
                                <input 
                                  type="text" 
                                  className="form-control form-control-sm" 
                                  value={editFormData.username}
                                  onChange={(e) => setEditFormData({...editFormData, username: e.target.value})}
                                />
                              </td>
                              <td>
                                <input 
                                  type="email" 
                                  className="form-control form-control-sm" 
                                  value={editFormData.email}
                                  onChange={(e) => setEditFormData({...editFormData, email: e.target.value})}
                                />
                              </td>
                              <td>
                                <input 
                                  type="text" 
                                  className="form-control form-control-sm" 
                                  value={editFormData.mobile}
                                  onChange={(e) => setEditFormData({...editFormData, mobile: e.target.value})}
                                />
                              </td>
                              <td>
                                <input 
                                  type="text" 
                                  className="form-control form-control-sm" 
                                  value={editFormData.address}
                                  onChange={(e) => setEditFormData({...editFormData, address: e.target.value})}
                                />
                              </td>
                              <td>
                                <input 
                                  type="text" 
                                  className="form-control form-control-sm" 
                                  value={editFormData.carPlateNo}
                                  onChange={(e) => setEditFormData({...editFormData, carPlateNo: e.target.value})}
                                />
                              </td>
                              <td>
                                <input 
                                  type="number" 
                                  step="0.01"
                                  className="form-control form-control-sm" 
                                  style={{width: '100px'}}
                                  value={editFormData.credit}
                                  onChange={(e) => setEditFormData({...editFormData, credit: parseFloat(e.target.value)})}
                                />
                              </td>
                              <td>
                                <button 
                                  className="btn btn-sm btn-success me-1" 
                                  onClick={() => handleUpdateUser(user.id)}
                                  title="Save Changes"
                                >
                                  <i className="fas fa-check"></i>
                                </button>
                                <button 
                                  className="btn btn-sm btn-secondary" 
                                  onClick={handleCancelEdit}
                                  title="Cancel"
                                >
                                  <i className="fas fa-times"></i>
                                </button>
                              </td>
                            </>
                          ) : (
                            <>
                              <td>{user.id}</td>
                              <td>{user.username}</td>
                              <td>{user.email}</td>
                              <td>{user.mobile}</td>
                              <td>{user.address}</td>
                              <td>{user.carPlateNo}</td>
                              <td>{user.credit?.toFixed(2) || '0.00'}</td>
                              <td>
                                <button 
                                  className="btn btn-sm btn-primary me-1" 
                                  onClick={() => handleEditUser(user)}
                                  title="Edit Customer"
                                >
                                  <i className="fas fa-edit"></i>
                                </button>
                                <button 
                                  className="btn btn-sm btn-danger" 
                                  onClick={() => handleDeleteUser(user.id)}
                                  title="Delete Customer"
                                >
                                  <i className="fas fa-trash-alt"></i>
                                </button>
                              </td>
                            </>
                          )}
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}

            {/* View Parking */}
            {activeView === 'parking' && (
              <div className="admin-table-wrapper">
                <table className="admin-table parking-table">
                  <thead>
                    <tr>
                      <th style={{fontWeight: 'bold'}}>Parking ID</th>
                      <th style={{textAlign: 'center'}}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {parkingSpots.length === 0 ? (
                      <tr>
                        <td colSpan="2" className="text-center">
                          <div className="admin-empty-state">
                            <p>🅿️</p>
                            <p>No parking spots found</p>
                            <p>Please contact system administrator</p>
                          </div>
                        </td>
                      </tr>
                    ) : (
                      parkingSpots.map(spot => (
                        <tr key={spot.parkingId}>
                          <td style={{fontWeight: 'bold'}}>{spot.parkingId}</td>
                          <td style={{textAlign: 'center'}}>
                            {getStatusBadge(spot.availability)}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
