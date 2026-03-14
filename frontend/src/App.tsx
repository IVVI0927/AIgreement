import React from 'react';
import Dashboard from './pages/Dashboard';
import './styles/accessibility.css';

function App() {
  return (
    <div className="App" role="application" aria-label="Legal AI Application">
      <main className="main-content" role="main">
        <Dashboard />
      </main>
    </div>
  );
}

export default App;
