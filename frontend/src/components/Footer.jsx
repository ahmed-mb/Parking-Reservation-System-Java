/**
 * Footer.jsx — Site-wide footer with Contact, About, and Created By sections.
 *
 * Renders a dark-themed footer that matches the existing Bootstrap 4 + custom
 * gradient style used throughout the application. Buttons and links are present
 * for layout/design purposes but are not wired to real handlers yet.
 */

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="container">
        <div className="row footer-content">

          {/* ----- About section ----- */}
          <div className="col-md-4 footer-col">
            <h5 className="footer-heading">
              <i className="fas fa-info-circle mr-2" />
              About
            </h5>
            <p className="footer-text">
              Parking Reservation System simplifies finding and reserving
              parking spots. Secure, fast, and available 24/7.
            </p>
            <button
              type="button"
              className="btn btn-outline-light btn-sm footer-btn"
              onClick={() => {}}
            >
              Learn More &raquo;
            </button>
          </div>

          {/* ----- Contact section ----- */}
          <div className="col-md-4 footer-col">
            <h5 className="footer-heading">
              <i className="fas fa-envelope mr-2" />
              Contact
            </h5>
            <ul className="footer-contact list-unstyled">
              <li>
                <i className="fas fa-map-marker-alt mr-2" />
                Parking Inc., 123 Main St, City
              </li>
              <li>
                <i className="fas fa-phone mr-2" />
                +1 (555) 123-4567
              </li>
              <li>
                <i className="fas fa-envelope mr-2" />
                support@parkingsystem.com
              </li>
            </ul>
            <button
              type="button"
              className="btn btn-outline-light btn-sm footer-btn"
              onClick={() => {}}
            >
              Send Message &raquo;
            </button>
          </div>

          {/* ----- Created By / Social section ----- */}
          <div className="col-md-4 footer-col">
            <h5 className="footer-heading">
              <i className="fas fa-code mr-2" />
              Created By
            </h5>
            <p className="footer-text">
              Built with care by the Parking System team.
            </p>
            <div className="footer-social">
              <button
                type="button"
                className="btn btn-social"
                aria-label="Facebook"
                onClick={() => {}}
              >
                <i className="fab fa-facebook-f" />
              </button>
              <button
                type="button"
                className="btn btn-social"
                aria-label="Twitter"
                onClick={() => {}}
              >
                <i className="fab fa-twitter" />
              </button>
              <button
                type="button"
                className="btn btn-social"
                aria-label="LinkedIn"
                onClick={() => {}}
              >
                <i className="fab fa-linkedin-in" />
              </button>
              <button
                type="button"
                className="btn btn-social"
                aria-label="GitHub"
                onClick={() => {}}
              >
                <i className="fab fa-github" />
              </button>
            </div>
          </div>
        </div>

        {/* ----- Bottom bar ----- */}
        <div className="footer-bottom">
          <hr className="footer-divider" />
          <p className="footer-copyright">
            &copy; {currentYear} Parking Reservation System. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
