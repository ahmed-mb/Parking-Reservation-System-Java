import React from 'react';

/**
 * ErrorBoundary — catches render/lifecycle errors anywhere in the tree
 * below it and shows a recovery UI instead of an unmounted, blank page.
 *
 * Must be a class component: getDerivedStateFromError/componentDidCatch
 * have no hooks equivalent in React 18.
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Unhandled error in component tree:', error, errorInfo);
  }

  handleReload = () => {
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          textAlign: 'center',
          padding: '24px',
        }}>
          <h1>Something went wrong</h1>
          <p style={{ color: '#666', marginBottom: '20px' }}>
            An unexpected error occurred. Please try reloading the page.
          </p>
          <button
            onClick={this.handleReload}
            className="btn btn-primary"
          >
            Reload
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
