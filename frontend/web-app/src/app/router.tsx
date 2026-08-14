/* oxlint-disable react/only-export-components */
import { lazy, Suspense, type ReactNode } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute'
import { AppShell } from '@/layouts/AppShell'
import { NotFoundPage } from '@/shared/pages/NotFoundPage'

const LoginPage = lazy(() =>
  import('@/features/auth/pages/LoginPage').then((module) => ({
    default: module.LoginPage,
  })),
)
const RegisterPage = lazy(() =>
  import('@/features/auth/pages/RegisterPage').then((module) => ({
    default: module.RegisterPage,
  })),
)
const OAuthCallbackPage = lazy(() =>
  import('@/features/auth/pages/OAuthCallbackPage').then((module) => ({
    default: module.OAuthCallbackPage,
  })),
)
const HomePage = lazy(() =>
  import('@/features/home/pages/HomePage').then((module) => ({
    default: module.HomePage,
  })),
)
const HitListPage = lazy(() =>
  import('@/features/hit/pages/HitListPage').then((module) => ({
    default: module.HitListPage,
  })),
)
const MessagesPage = lazy(() =>
  import('@/features/messages/pages/MessagesPage').then((module) => ({
    default: module.MessagesPage,
  })),
)
const MembershipPlansPage = lazy(() =>
  import('@/features/membership/pages/MembershipPlansPage').then((module) => ({
    default: module.MembershipPlansPage,
  })),
)
const MembershipCenterPage = lazy(() =>
  import('@/features/membership/pages/MembershipCenterPage').then((module) => ({
    default: module.MembershipCenterPage,
  })),
)
const MembershipOrderPage = lazy(() =>
  import('@/features/membership/pages/MembershipOrderPage').then((module) => ({
    default: module.MembershipOrderPage,
  })),
)
const MembershipOrdersPage = lazy(() =>
  import('@/features/membership/pages/MembershipOrdersPage').then((module) => ({
    default: module.MembershipOrdersPage,
  })),
)
const QuestionBankPage = lazy(() =>
  import('@/features/question-bank/pages/QuestionBankPage').then((module) => ({
    default: module.QuestionBankPage,
  })),
)
const CertificateExamPage = lazy(() =>
  import('@/features/question/pages/CertificateExamPage').then((module) => ({
    default: module.CertificateExamPage,
  })),
)
const CertificatePracticePage = lazy(() =>
  import('@/features/question/pages/CertificatePracticePage').then(
    (module) => ({ default: module.CertificatePracticePage }),
  ),
)
const InterviewPracticePage = lazy(() =>
  import('@/features/question/pages/InterviewPracticePage').then((module) => ({
    default: module.InterviewPracticePage,
  })),
)
const QuestionReviewPage = lazy(() =>
  import('@/features/question/pages/QuestionReviewPage').then((module) => ({
    default: module.QuestionReviewPage,
  })),
)
const UserCenterPage = lazy(() =>
  import('@/features/user-center/pages/UserCenterPage').then((module) => ({
    default: module.UserCenterPage,
  })),
)
const UserProfileEditPage = lazy(() =>
  import('@/features/user-center/pages/UserProfileEditPage').then((module) => ({
    default: module.UserProfileEditPage,
  })),
)
const UserQuestionLibraryPage = lazy(() =>
  import('@/features/user-center/pages/UserQuestionLibraryPage').then(
    (module) => ({ default: module.UserQuestionLibraryPage }),
  ),
)
const PublicUserProfilePage = lazy(() =>
  import('@/features/user-profile/pages/PublicUserProfilePage').then(
    (module) => ({ default: module.PublicUserProfilePage }),
  ),
)

function route(element: ReactNode) {
  return (
    <Suspense
      fallback={
        <div className="app-container py-8">
          <div className="h-72 animate-pulse rounded-2xl bg-white/70" />
        </div>
      }
    >
      {element}
    </Suspense>
  )
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: route(<LoginPage />),
  },
  {
    path: '/register',
    element: route(<RegisterPage />),
  },
  {
    path: '/oauth/callback/:provider',
    element: route(<OAuthCallbackPage />),
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          {
            index: true,
            element: <Navigate to="/home" replace />,
          },
          {
            path: '/home',
            element: route(<HomePage />),
          },
          {
            path: '/banks/interview',
            element: route(<QuestionBankPage kind="interview" />),
          },
          {
            path: '/banks/certification',
            element: route(<QuestionBankPage kind="certification" />),
          },
          {
            path: '/banks/interview/:bankId/practice',
            element: route(<InterviewPracticePage />),
          },
          {
            path: '/banks/certification/:bankId/practice',
            element: route(<CertificatePracticePage />),
          },
          {
            path: '/banks/certification/exams/:sessionId',
            element: route(<CertificateExamPage />),
          },
          {
            path: '/banks/:groupType/:bankId/review',
            element: route(<QuestionReviewPage />),
          },
          {
            path: '/hits',
            element: route(<HitListPage />),
          },
          {
            path: '/me',
            element: route(<UserCenterPage />),
          },
          {
            path: '/me/edit',
            element: route(<UserProfileEditPage />),
          },
          {
            path: '/me/wrong-questions',
            element: route(<UserQuestionLibraryPage kind="wrong" />),
          },
          {
            path: '/me/favorites',
            element: route(<UserQuestionLibraryPage kind="favorite" />),
          },
          {
            path: '/me/notes',
            element: route(<UserQuestionLibraryPage kind="note" />),
          },
          {
            path: '/users/:userId',
            element: route(<PublicUserProfilePage />),
          },
          {
            path: '/messages',
            element: route(<MessagesPage />),
          },
          {
            path: '/membership',
            element: route(<MembershipPlansPage />),
          },
          {
            path: '/membership/center',
            element: route(<MembershipCenterPage />),
          },
          {
            path: '/membership/orders',
            element: route(<MembershipOrdersPage />),
          },
          {
            path: '/membership/orders/:orderNo',
            element: route(<MembershipOrderPage />),
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])
