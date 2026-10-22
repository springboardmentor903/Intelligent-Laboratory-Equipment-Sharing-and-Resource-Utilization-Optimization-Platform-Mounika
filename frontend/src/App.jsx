import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/common/Navbar';
import ProtectedRoute from './components/common/ProtectedRoute';
import RoleGuard from './components/common/RoleGuard';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import Profile from './pages/Profile';
import EquipmentCatalog from './pages/EquipmentCatalog';
import EquipmentDetail from './pages/EquipmentDetail';
import MyBookings from './pages/MyBookings';
import PendingApprovals from './pages/PendingApprovals';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<EquipmentCatalog />} />
              <Route path="/equipment/:id" element={<EquipmentDetail />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />

              {/* Authenticated Routes */}
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <Profile />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/my-bookings"
                element={
                  <ProtectedRoute>
                    <MyBookings />
                  </ProtectedRoute>
                }
              />

              {/* Role-Protected Routes for Lab Management */}
              <Route
                path="/pending-approvals"
                element={
                  <RoleGuard
                    allowedRoles={[
                      'ROLE_LAB_MANAGER',
                      'ROLE_DEPARTMENT_HEAD',
                      'ROLE_INSTITUTION_ADMIN',
                      'ROLE_SYSTEM_ADMIN',
                    ]}
                  >
                    <PendingApprovals />
                  </RoleGuard>
                }
              />

              {/* Catch-all */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>

          {/* Footer */}
          <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
            <div className="max-w-7xl mx-auto px-4">
              Lab Resource Utilization Platform • Developed for Multi-Institution Equipment Sharing &amp; Optimization
            </div>
          </footer>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
