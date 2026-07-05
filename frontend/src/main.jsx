/**
 * main.jsx — React DOM entry point.
 *
 * Mounts the root <App /> component into the #root element, wrapped in
 * React.StrictMode and an ErrorBoundary, and pulls in the global stylesheet.
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>,
)
