import React, { useState, useEffect } from 'react';
import { contractService } from '../services/contractService';

interface DashboardStats {
  totalContracts: number;
  pendingReview: number;
  highRiskContracts: number;
  recentActivity: number;
}

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalContracts: 0,
    pendingReview: 0,
    highRiskContracts: 0,
    recentActivity: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const data = await contractService.getDashboardStats() as Partial<DashboardStats>;
      setStats({
        totalContracts: data.totalContracts ?? 0,
        pendingReview: data.pendingReview ?? 0,
        highRiskContracts: data.highRiskContracts ?? 0,
        recentActivity: data.recentActivity ?? 0
      });
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="dashboard" role="region" aria-label="Dashboard">
      <header className="dashboard-header">
        <h1>Contract Dashboard</h1>
        <p className="role-badge" aria-label="Environment">Frontend status: connected</p>
      </header>

      <section className="stats-grid" aria-label="Statistics">
        <div className="stat-card" tabIndex={0}>
          <h3>Total Contracts</h3>
          <p className="stat-value">{stats.totalContracts}</p>
        </div>
        <div className="stat-card" tabIndex={0}>
          <h3>Pending Review</h3>
          <p className="stat-value">{stats.pendingReview}</p>
        </div>
        <div className="stat-card" tabIndex={0}>
          <h3>High Risk</h3>
          <p className="stat-value">{stats.highRiskContracts}</p>
        </div>
        <div className="stat-card" tabIndex={0}>
          <h3>Recent Activity</h3>
          <p className="stat-value">{stats.recentActivity}</p>
        </div>
      </section>

      <section className="contracts-section" aria-label="Recent contracts">
        <h2>Service Check</h2>
        {loading ? (
          <div role="status" aria-live="polite">Loading dashboard data...</div>
        ) : (
          <div role="status" aria-live="polite">Dashboard data loaded.</div>
        )}
      </section>
    </div>
  );
};

export default Dashboard;
