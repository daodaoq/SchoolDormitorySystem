import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import BasicLayout from './layouts/BasicLayout';
import Dashboard from './pages/Dashboard';
import Student from './pages/Student';
import FeeItem from './pages/FeeItem';
import Bill from './pages/Bill';
import Payment from './pages/Payment';
import Statistics from './pages/Statistics';
import AiQa from './pages/AiQa';

function App() {
  return (
    <Routes>
      <Route path="/" element={<BasicLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="students" element={<Student />} />
        <Route path="fee-items" element={<FeeItem />} />
        <Route path="bills" element={<Bill />} />
        <Route path="payments" element={<Payment />} />
        <Route path="statistics" element={<Statistics />} />
        <Route path="ai-qa" element={<AiQa />} />
      </Route>
    </Routes>
  );
}

export default App;
