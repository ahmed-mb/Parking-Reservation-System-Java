import { Link } from 'react-router-dom';
import '../not-found.css';

export default function NotFound() {
  return (
    <div className="error-container">
      <div className="error-content">
        {/* Animated 404 */}
        <div className="error-code">
          <span className="digit">4</span>
          <span className="digit parking-icon">🚗</span>
          <span className="digit">4</span>
        </div>

        {/* Error Message */}
        <h1 className="error-title">Oops! Parking Spot Not Found</h1>
        <p className="error-description">
          The page you're looking for seems to have been relocated to a different parking zone.
          <br />
          Don't worry, we'll help you find your way back!
        </p>

        {/* Action Buttons */}
        <div className="error-actions">
          <Link to="/" className="btn-home">
            <i className="fas fa-home"></i>
            <span>Back to Home</span>
          </Link>
          <Link to="/login" className="btn-login">
            <i className="fas fa-sign-in-alt"></i>
            <span>Login</span>
          </Link>
          <Link to="/register" className="btn-register">
            <i className="fas fa-user-plus"></i>
            <span>Register</span>
          </Link>
        </div>

        {/* Helpful Links */}
        <div className="helpful-links">
          <h3>Quick Navigation</h3>
          <div className="links-grid">
            <Link to="/" className="link-card">
              <i className="fas fa-home"></i>
              <span>Home Page</span>
            </Link>
            <Link to="/login" className="link-card">
              <i className="fas fa-sign-in-alt"></i>
              <span>Login</span>
            </Link>
            <Link to="/register" className="link-card">
              <i className="fas fa-user-plus"></i>
              <span>Register</span>
            </Link>
            <a href="#" className="link-card" onClick={(e) => { e.preventDefault(); window.history.back(); }}>
              <i className="fas fa-arrow-left"></i>
              <span>Go Back</span>
            </a>
          </div>
        </div>

        {/* Error Details */}
        <div className="error-details">
          <p className="error-code-text">Error Code: 404</p>
          <p className="error-timestamp">
            <i className="fas fa-clock"></i>
            <span>{new Date().toLocaleString()}</span>
          </p>
        </div>
      </div>

      {/* Animated Background */}
      <div className="animated-bg">
        <div className="parking-spot spot-1">🚗</div>
        <div className="parking-spot spot-2">🚗</div>
        <div className="parking-spot spot-3">🚗</div>
        <div className="parking-spot spot-4">🚗</div>
        <div className="parking-spot spot-5">🚗</div>
      </div>
    </div>
  );
}
