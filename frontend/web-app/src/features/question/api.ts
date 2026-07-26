import { apiRequest } from '@/shared/api/client'
import type {
  ActionStatusValue,
  GroupTypeValue,
  QuestionTypeValue,
} from '@/shared/constants/domain'
import type {
  AiChat,
  AiFollowUpInput,
  BankFinish,
  CertificateAnswer,
  CertificateExam,
  CertificateQuestion,
  CertificateReview,
  InterviewAnswer,
  InterviewQuestion,
  InterviewReview,
} from '@/features/question/types'

export function getInterviewQuestions(bankId: number) {
  return apiRequest<InterviewQuestion[]>({
    url: '/app/bank/questions/interview/question',
    params: { bankId },
  })
}

export function submitInterviewAnswer(data: {
  bankId: number
  questionId: number
  content: string
}) {
  return apiRequest<InterviewAnswer>({
    url: '/app/bank/questions/interview/answer',
    method: 'POST',
    data,
  })
}

export function getInterviewRecord(bankId: number) {
  return apiRequest<InterviewReview[]>({
    url: '/app/bank/questions/interview/record',
    params: { bankId },
  })
}

export function getCertificateQuestions(bankId: number) {
  return apiRequest<CertificateQuestion[]>({
    url: '/app/bank/questions/certificate/question',
    params: { bankId },
  })
}

export function submitCertificatePracticeAnswer(data: {
  bankId: number
  questionId: number
  questionType: QuestionTypeValue
  chosenOptions: string[]
}) {
  return apiRequest<CertificateAnswer>({
    url: '/app/bank/questions/certificate/practice/answer',
    method: 'POST',
    data,
  })
}

export function getInterviewReview(bankId: number) {
  return apiRequest<InterviewReview[]>({
    url: '/app/bank/questions/interview/review',
    params: { bankId },
  })
}

export function getCertificateReview(bankId: number) {
  return apiRequest<CertificateReview[]>({
    url: '/app/bank/questions/certificate/review',
    params: { bankId },
  })
}

export function finishQuestionBank(bankId: number, groupType: GroupTypeValue) {
  return apiRequest<BankFinish>({
    url: '/app/bank/questions/finish',
    method: 'POST',
    params: { bankId, groupType },
  })
}

export function saveQuestionNote(data: {
  bankId: number
  questionId: number
  noteContent: string
}) {
  return apiRequest<void>({
    url: '/app/bank/questions/answer/note',
    method: 'POST',
    data,
  })
}

export function updateQuestionFavorite(
  bankId: number,
  questionId: number,
  actionStatus: ActionStatusValue,
) {
  return apiRequest<void>({
    url: '/app/bank/questions/collect',
    method: 'POST',
    params: { bankId, questionId, actionStatus },
  })
}

export function getAiChat(bankId: number, bankType: GroupTypeValue) {
  return apiRequest<AiChat>({
    url: '/app/bank/questions/ai/chat',
    params: { bankId, bankType },
  })
}

export function sendAiFollowUp(data: AiFollowUpInput) {
  return apiRequest<AiChat>({
    url: '/app/bank/questions/ai/chat',
    method: 'POST',
    data,
  })
}

export function closeAiChat(bankId: number) {
  return apiRequest<void>({
    url: '/app/bank/questions/ai/chat/close',
    method: 'POST',
    params: { bankId },
  })
}

export function startCertificateExam(bankId: number) {
  return apiRequest<CertificateExam>({
    url: '/app/bank/certificate/exams/start',
    method: 'POST',
    params: { bankId },
  })
}

export function getCertificateExam(sessionId: number) {
  return apiRequest<CertificateExam>({
    url: `/app/bank/certificate/exams/${sessionId}`,
  })
}

export function saveCertificateExamAnswer(data: {
  sessionId: number
  questionId: number
  chosenOptions: string[]
}) {
  return apiRequest<void>({
    url: '/app/bank/certificate/exams/answer',
    method: 'POST',
    data,
  })
}

export function submitCertificateExam(sessionId: number) {
  return apiRequest<BankFinish>({
    url: `/app/bank/certificate/exams/${sessionId}/submit`,
    method: 'POST',
  })
}
