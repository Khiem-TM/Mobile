import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { useAuth } from './lib/auth';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { AuditPage } from './pages/AuditPage';
import { BlogDetailPage } from './pages/BlogDetailPage';
import { BlogsPage } from './pages/BlogsPage';
import { DashboardPage } from './pages/DashboardPage';
import { ExerciseDetailPage } from './pages/ExerciseDetailPage';
import { ExercisesPage } from './pages/ExercisesPage';
import { FoodDetailPage } from './pages/FoodDetailPage';
import { FoodsPage } from './pages/FoodsPage';
import { LoginPage } from './pages/LoginPage';
import { SystemPage } from './pages/SystemPage';
import { UserDetailPage } from './pages/UserDetailPage';
import { UsersPage } from './pages/UsersPage';
import { WarningsPage } from './pages/WarningsPage';

function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate replace state={{ from: location }} to="/login" />;
  return <Outlet />;
}

function PublicOnlyRoute() {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) return <Navigate replace to="/" />;
  return <Outlet />;
}

export default function App() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route element={<LoginPage />} path="/login" />
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route element={<DashboardPage />} index />
          <Route element={<UsersPage />} path="users" />
          <Route element={<UserDetailPage />} path="users/:id" />
          <Route element={<FoodsPage />} path="foods" />
          <Route element={<FoodDetailPage />} path="foods/:id" />
          <Route element={<Navigate replace to="/foods" />} path="foods/pending" />
          <Route element={<ExercisesPage />} path="exercises" />
          <Route element={<ExerciseDetailPage />} path="exercises/:id" />
          <Route element={<BlogsPage />} path="blogs" />
          <Route element={<BlogDetailPage />} path="blogs/:id" />
          <Route element={<WarningsPage />} path="warnings" />
          <Route element={<AnalyticsPage />} path="analytics" />
          <Route element={<SystemPage />} path="system" />
          <Route element={<AuditPage />} path="audit" />
        </Route>
      </Route>
      <Route element={<Navigate replace to="/" />} path="*" />
    </Routes>
  );
}
