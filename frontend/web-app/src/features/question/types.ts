import type {
  GroupTypeValue,
  QuestionTypeValue,
} from '@/shared/constants/domain'

export interface AiEvaluation {
  scoreRate: number | null
  accurateComment: string | null
  innovativeComment: string | null
  missingComment: string | null
  wrongComment: string | null
  summary: string | null
  modelName: string | null
}

export interface InterviewQuestion {
  questionId: number
  title: string
  questionType: QuestionTypeValue
  isFavorite: boolean
}

export interface InterviewAnswer {
  questionId: number
  analysis: string
  aiResult: AiEvaluation | null
  aiEvaluationEnabled: boolean
  isFavorite: boolean
  content: string
}

export interface InterviewReview extends InterviewQuestion {
  analysis: string
  aiResult: AiEvaluation | null
  isCorrect: boolean | null
  content: string
}

export interface CertificateQuestion {
  questionId: number
  title: string
  options: string[]
  questionType: QuestionTypeValue
  imageUrl: string | null
  isFavorite: boolean
}

export interface CertificateAnswer {
  correctAnswer: string[]
  analysis: string
  questionId: number
  correct: boolean
  isFavorite: boolean
}

export interface CertificateReview extends CertificateQuestion {
  chosenOptions: string[]
  correctAnswer: string[]
  analysis: string
  isCorrect: boolean
}

export interface CertificateExamQuestion extends CertificateQuestion {
  chosenOptions: string[]
  answered: boolean
}

export interface CertificateExam {
  sessionId: number
  bankId: number
  expiresAt: string
  status: number
  questions: CertificateExamQuestion[]
}

export interface QuestionCount {
  totalCount: number
  answeredCount: number
  correctCount: number | null
  correctRate: number | null
}

export interface BankFinish {
  interviewQuestionReviewVos: InterviewReview[] | null
  certificateQuestionReviewVos: CertificateReview[] | null
  questionCount: QuestionCount
}

export interface AiChatMessage {
  messageId: number
  senderType: number
  messageContent: string
  createdTime: string
}

export interface AiChat {
  sessionId: number | null
  bankId: number
  messages: AiChatMessage[]
}

export interface AiFollowUpInput {
  bankId: number
  questionId: number
  groupType: GroupTypeValue
  message: string
}
