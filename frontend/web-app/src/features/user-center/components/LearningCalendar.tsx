import { useQuery } from '@tanstack/react-query'
import { eachDayOfInterval, endOfYear, format, startOfYear } from 'date-fns'
import { getLearningCalendar } from '@/features/user-center/api'
import { cn } from '@/shared/lib/cn'
import { Card } from '@/shared/ui/Card'

function activityColor(minutes: number | undefined) {
  if (minutes == null) return 'bg-[#ebe5e0]'
  if (minutes === 0) return 'bg-[#dcebe5]'
  if (minutes <= 30) return 'bg-[#bad8cd]'
  if (minutes <= 60) return 'bg-[#91c1b1]'
  if (minutes <= 120) return 'bg-[#659e8b]'
  return 'bg-[#3f7563]'
}

export function LearningCalendar() {
  const year = new Date().getFullYear()
  const calendarQuery = useQuery({
    queryKey: ['learning-calendar', year],
    queryFn: () => getLearningCalendar(year),
  })
  const valueByDate = new Map(
    calendarQuery.data?.map((item) => [item.date, item.studyMinutes]) || [],
  )
  const days = eachDayOfInterval({
    start: startOfYear(new Date(year, 0, 1)),
    end: endOfYear(new Date(year, 0, 1)),
  })
  const leadingEmptyCells = Array.from({
    length: startOfYear(new Date(year, 0, 1)).getDay(),
  })

  return (
    <Card className="overflow-hidden p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-extrabold">{year} 学习日历</h2>
          <p className="mt-1 text-xs text-muted">颜色越深，学习时间越长</p>
        </div>
        <div className="flex items-center gap-1 text-[10px] text-muted">
          少
          {[0, 10, 45, 90, 150].map((minutes) => (
            <span
              key={minutes}
              className={cn('size-3 rounded-sm', activityColor(minutes))}
            />
          ))}
          多
        </div>
      </div>
      <div className="mt-5 overflow-x-auto pb-2">
        <div
          className="grid min-w-[720px] grid-flow-col grid-rows-7 gap-1.5"
          aria-label={`${year}年学习日历`}
        >
          {leadingEmptyCells.map((_, index) => (
            <span key={`empty-${index}`} className="size-3" />
          ))}
          {days.map((day) => {
            const date = format(day, 'yyyy-MM-dd')
            const minutes = valueByDate.get(date)
            return (
              <span
                key={date}
                className={cn('size-3 rounded-[3px]', activityColor(minutes))}
                title={`${date}：${minutes == null ? '无记录' : `${minutes} 分钟`}`}
                aria-label={`${date}，${minutes == null ? '无学习记录' : `学习${minutes}分钟`}`}
              />
            )
          })}
        </div>
      </div>
    </Card>
  )
}
