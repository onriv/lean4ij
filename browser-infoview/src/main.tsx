import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(



    // Strict Mode:
    //
    // If your app is running in <StrictMode>, React intentionally renders twice in development mode. This behavior helps identify potential issues and improve code quality.
    // To avoid this, consider removing <StrictMode> during development.

        // <React.StrictMode>
    <App />
  // </React.StrictMode>,
)
