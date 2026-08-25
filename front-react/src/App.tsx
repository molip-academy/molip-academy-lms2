import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import type { ReactElement } from "react";
import { AuthProvider, useAuth } from "@/auth/AuthContext";
import { JournalPage } from "@/pages/JournalPage";
import { LoginPage } from "@/pages/LoginPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { SignupPage } from "@/pages/SignupPage";

function RequireAuth({ children }: { children: ReactElement }) {
  const { member, loading } = useAuth();
  if (loading) return <FullPageMessage text="불러오는 중…" />;
  if (!member) return <Navigate to="/login" replace />;
  return children;
}

function RedirectIfAuthed({ children }: { children: ReactElement }) {
  const { member, loading } = useAuth();
  if (loading) return <FullPageMessage text="불러오는 중…" />;
  if (member) return <Navigate to="/" replace />;
  return children;
}

function FullPageMessage({ text }: { text: string }) {
  return (
    <div className="flex min-h-svh items-center justify-center text-sm text-muted-foreground">{text}</div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<RequireAuth><JournalPage /></RequireAuth>} />
          <Route path="/me" element={<RequireAuth><ProfilePage /></RequireAuth>} />
          <Route path="/login" element={<RedirectIfAuthed><LoginPage /></RedirectIfAuthed>} />
          <Route path="/signup" element={<RedirectIfAuthed><SignupPage /></RedirectIfAuthed>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
