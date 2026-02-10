import { useState, useEffect } from 'react';
import axios from 'axios';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useModernAlert } from './ModernAlert';
import '../customer-dashboard.css';

export default function UserProfile() {
  const [userInfo, setUserInfo] = useState(null);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [loading, setLoading] = useState(true);
  const { logout } = useAuth();
  const navigate = useNavigate();
  const modernAlert = useModernAlert();

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const fetchUserInfo = async () => {
    try {
      const res = await axios.get('/api/users/me');
      setUserInfo(res.data);
      setFormData(res.data);
    } catch (err) {
      console.error('Error fetching user info:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = () => {
    setFormData({ ...userInfo });
    setEditing(true);
  };

  const handleCancelEdit = () => {
    setEditing(false);
    setFormData({ ...userInfo });
  };

  const handleUpdate = async () => {
    try {
      await axios.put(`/api/users/${userInfo.id}`, formData);
      modernAlert.success('Profile Updated', 'Your profile has been updated successfully!');
      setEditing(false);
      fetchUserInfo();
    } catch (err) {
      modernAlert.error('Update Failed', err.response?.data?.message || 'Update failed. Please try again.');
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
              <li className="nav-item">
                <Link className="nav-link" to="/dashboard">User page</Link>
              </li>
              <li className="nav-item">
                <Link className="nav-link" to="/history">History</Link>
              </li>
              <li className="nav-item active">
                <Link className="nav-link" to="/profile">
                  Profile <span className="sr-only">(current)</span>
                </Link>
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

      {/* Main Content - User Profile Table (Matching C_userInfo.aspx GridView) */}
      <div className="customer-table-container">
        <div className="customer-table-wrapper">
          <table className="customer-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Address</th>
                <th>Car Plate</th>
                <th>Balance Credit</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {!userInfo ? (
                <tr>
                  <td colSpan="8" className="text-center">
                    <div className="customer-empty-state">
                      <p>&#x1F464;</p>
                      <p>No profile information found</p>
                      <p>Please contact support if this issue persists</p>
                    </div>
                  </td>
                </tr>
              ) : (
                <tr>
                  <td>{userInfo.id}</td>
                  {editing ? (
                    <>
                      <td>
                        <input 
                          type="text" 
                          value={formData.username || ''}
                          onChange={(e) => setFormData({...formData, username: e.target.value})}
                        />
                      </td>
                      <td>
                        <input 
                          type="email" 
                          value={formData.email || ''}
                          onChange={(e) => setFormData({...formData, email: e.target.value})}
                        />
                      </td>
                      <td>
                        <input 
                          type="text" 
                          value={formData.mobile || ''}
                          onChange={(e) => setFormData({...formData, mobile: e.target.value})}
                        />
                      </td>
                      <td>
                        <input 
                          type="text" 
                          value={formData.address || ''}
                          onChange={(e) => setFormData({...formData, address: e.target.value})}
                        />
                      </td>
                      <td>
                        <input 
                          type="text" 
                          value={formData.carPlateNo || ''}
                          onChange={(e) => setFormData({...formData, carPlateNo: e.target.value})}
                        />
                      </td>
                      <td>{userInfo.credit != null ? Number(userInfo.credit).toFixed(2) : '0.00'}</td>
                      <td>
                        <button 
                          className="icon-btn btn-save" 
                          onClick={handleUpdate}
                          title="Save Changes"
                        >
                          <i className="fas fa-check"></i>
                        </button>
                        <button 
                          className="icon-btn btn-cancel" 
                          onClick={handleCancelEdit}
                          title="Cancel"
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      </td>
                    </>
                  ) : (
                    <>
                      <td>{userInfo.username}</td>
                      <td>{userInfo.email}</td>
                      <td>{userInfo.mobile || ''}</td>
                      <td>{userInfo.address || ''}</td>
                      <td>{userInfo.carPlateNo || ''}</td>
                      <td>{userInfo.credit != null ? Number(userInfo.credit).toFixed(2) : '0.00'}</td>
                      <td>
                        <button 
                          className="icon-btn btn-edit" 
                          onClick={handleEdit}
                          title="Edit Profile"
                        >
                          <i className="fas fa-edit"></i>
                        </button>
                      </td>
                    </>
                  )}
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
