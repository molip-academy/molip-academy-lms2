import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError, api } from "@/lib/api";
import { MOODS, MOOD_LABELS } from "@/lib/mood";
import type { Mood } from "@/lib/mood";
import { toHoursAndMinutes, toMinutes } from "@/lib/time";
import { todayIso } from "@/lib/date";
import type { JournalLookup } from "@/lib/types";
import { AppHeader } from "@/components/AppHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";

type Duration = { hours: string; minutes: string };

const EMPTY_DURATION: Duration = { hours: "", minutes: "" };

export function JournalPage() {
  const navigate = useNavigate();

  // 날짜는 URL이 소유한다 (ADR 0002와 같은 모양). 그래서 북마크·공유·뒤로가기가 동작한다.
  const { date = todayIso() } = useParams<{ date: string }>();
  const [sleep, setSleep] = useState<Duration>(EMPTY_DURATION);
  const [study, setStudy] = useState<Duration>(EMPTY_DURATION);
  const [exercise, setExercise] = useState<Duration>(EMPTY_DURATION);
  const [napCount, setNapCount] = useState("");
  const [rate, setRate] = useState("");
  const [mood, setMood] = useState<Mood | "">("");
  const [slowThinking, setSlowThinking] = useState("");
  const [thoughtBeforeSleep, setThoughtBeforeSleep] = useState("");
  const [vitaminTaken, setVitaminTaken] = useState("");
  const [content, setContent] = useState("");

  const [exists, setExists] = useState(false);
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<{ tone: "ok" | "bad"; text: string } | null>(null);

  const clearForm = useCallback(() => {
    setSleep(EMPTY_DURATION);
    setStudy(EMPTY_DURATION);
    setExercise(EMPTY_DURATION);
    setNapCount("");
    setRate("");
    setMood("");
    setSlowThinking("");
    setThoughtBeforeSleep("");
    setVitaminTaken("");
    setContent("");
  }, []);

  const load = useCallback(
    async (target: string, announce: boolean) => {
      setBusy(true);
      setErrors({});
      if (announce) setNotice(null);
      try {
        const result = await api.get<JournalLookup>(`/api/v1/journals/${target}`);
        setExists(result.exists);
        if (!result.exists || !result.journal) {
          clearForm();
          if (announce) setNotice({ tone: "ok", text: "이 날짜에는 아직 일지가 없습니다." });
          return;
        }
        const j = result.journal;
        setSleep(toHoursAndMinutes(j.sleepMinutes));
        setStudy(toHoursAndMinutes(j.studyMinutes));
        setExercise(toHoursAndMinutes(j.exerciseMinutes));
        setNapCount(j.napCount === null ? "" : String(j.napCount));
        setRate(j.oneSecondRuleRate === null ? "" : String(j.oneSecondRuleRate));
        setMood(j.mood ?? "");
        setSlowThinking(toChoice(j.slowThinking));
        setThoughtBeforeSleep(toChoice(j.thoughtBeforeSleep));
        setVitaminTaken(toChoice(j.vitaminTaken));
        setContent(j.content ?? "");
        if (announce) setNotice({ tone: "ok", text: "일지를 불러왔습니다." });
      } catch (error) {
        setNotice({ tone: "bad", text: error instanceof ApiError ? error.message : "불러오지 못했습니다." });
      } finally {
        setBusy(false);
      }
    },
    [clearForm],
  );

  useEffect(() => {
    void load(date, false);
  }, [load, date]);

  async function save() {
    setBusy(true);
    setErrors({});
    setNotice(null);
    try {
      await api.put(`/api/v1/journals/${date}`, {
        sleepMinutes: toMinutes(sleep.hours, sleep.minutes),
        studyMinutes: toMinutes(study.hours, study.minutes),
        exerciseMinutes: toMinutes(exercise.hours, exercise.minutes),
        napCount: napCount === "" ? null : Number(napCount),
        oneSecondRuleRate: rate === "" ? null : Number(rate),
        mood: mood === "" ? null : mood,
        slowThinking: fromChoice(slowThinking),
        thoughtBeforeSleep: fromChoice(thoughtBeforeSleep),
        vitaminTaken: fromChoice(vitaminTaken),
        content: content === "" ? null : content,
      });
      setExists(true);
      setNotice({ tone: "ok", text: "저장했습니다." });
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors(error.errors);
        setNotice({ tone: "bad", text: error.message });
      } else {
        setNotice({ tone: "bad", text: "저장하지 못했습니다." });
      }
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm(`${date} 일지를 삭제할까요? 되돌릴 수 없습니다.`)) return;
    setBusy(true);
    try {
      await api.delete(`/api/v1/journals/${date}`);
      clearForm();
      setExists(false);
      setNotice({ tone: "ok", text: "삭제했습니다." });
      navigate("/");
    } catch (error) {
      setNotice({ tone: "bad", text: error instanceof ApiError ? error.message : "삭제하지 못했습니다." });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <AppHeader />

      <h1 className="mb-8 text-center text-4xl font-bold tracking-tight">몰입 일지</h1>

      <div className="rounded-xl border p-5 shadow-sm">
        <div className="mb-6 flex flex-wrap items-center gap-3">
          <Input
            type="date"
            value={date}
            onChange={(e) => e.target.value && navigate(`/journals/${e.target.value}`)}
            className="w-auto flex-1 min-w-45"
          />
          <Button onClick={() => void load(date, true)} disabled={busy} className="bg-emerald-700 hover:bg-emerald-800">
            조회
          </Button>
        </div>

        <div className="space-y-4">
          <DurationRow label="수면시간" value={sleep} onChange={setSleep} error={errors.sleepMinutes} />
          <DurationRow label="공부시간" value={study} onChange={setStudy} error={errors.studyMinutes} />
          <DurationRow label="운동시간" value={exercise} onChange={setExercise} error={errors.exerciseMinutes} />

          <Row label="선잠횟수" unit="회" error={errors.napCount}>
            <Input
              type="number"
              min={0}
              value={napCount}
              onChange={(e) => setNapCount(e.target.value)}
              placeholder="횟수"
              className="w-40"
            />
          </Row>

          <Row label="1초원칙 준수" unit="%" error={errors.oneSecondRuleRate}>
            <Input
              type="number"
              step="0.1"
              min={0}
              max={100}
              value={rate}
              onChange={(e) => setRate(e.target.value)}
              placeholder="예: 95.5"
              className="w-40"
            />
          </Row>

          <Row label="기분상태">
            <NativeSelect value={mood} onChange={(v) => setMood(v as Mood | "")} className="w-64">
              <option value="">선택하지 않음</option>
              {MOODS.map((m) => (
                <option key={m} value={m}>
                  {MOOD_LABELS[m]}
                </option>
              ))}
            </NativeSelect>
          </Row>

          <YesNoRow label="슬로싱킹" value={slowThinking} onChange={setSlowThinking} />
          <YesNoRow label="취침전생각" value={thoughtBeforeSleep} onChange={setThoughtBeforeSleep} />
          <YesNoRow label="비타민복용" value={vitaminTaken} onChange={setVitaminTaken} />
        </div>

        <Textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="오늘의 몰입 기록을 남겨 주세요."
          className="mt-6 min-h-70"
        />
        {errors.content && <p className="mt-2 text-sm text-destructive">{errors.content}</p>}
      </div>

      {notice && (
        <p
          className={`mt-4 rounded-md px-4 py-3 text-sm ${
            notice.tone === "ok"
              ? "bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200"
              : "bg-red-50 text-red-800 dark:bg-red-950 dark:text-red-200"
          }`}
        >
          {notice.text}
        </p>
      )}

      <Button
        onClick={() => void save()}
        disabled={busy}
        className="mt-5 w-full bg-emerald-700 py-6 text-base hover:bg-emerald-800"
      >
        저장하기
      </Button>

      {/* 일지가 없는 날짜에는 삭제할 것이 없으므로 보이지 않는다. */}
      {exists && (
        <Button variant="ghost" onClick={() => void remove()} disabled={busy} className="mt-2 w-full text-destructive">
          이 날짜 일지 삭제
        </Button>
      )}
    </div>
  );
}

function Row({
  label,
  unit,
  error,
  children,
}: {
  label: string;
  unit?: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <div className="flex items-center gap-4">
        <div className="w-38 shrink-0 font-semibold">
          {label}
          {unit && <span className="ml-1 text-xs font-normal text-muted-foreground">({unit})</span>}
        </div>
        <div className="flex items-center gap-2">{children}</div>
      </div>
      {error && <p className="mt-1 ml-38 text-sm text-destructive">{error}</p>}
    </div>
  );
}

function DurationRow({
  label,
  value,
  onChange,
  error,
}: {
  label: string;
  value: Duration;
  onChange: (next: Duration) => void;
  error?: string;
}) {
  return (
    <Row label={label} unit="HH:MM" error={error}>
      <Input
        type="number"
        min={0}
        value={value.hours}
        onChange={(e) => onChange({ ...value, hours: e.target.value })}
        placeholder="시"
        className="w-24 text-center"
      />
      <span className="text-muted-foreground">:</span>
      <Input
        type="number"
        min={0}
        max={59}
        value={value.minutes}
        onChange={(e) => onChange({ ...value, minutes: e.target.value })}
        placeholder="분"
        className="w-24 text-center"
      />
    </Row>
  );
}

function YesNoRow({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (next: string) => void;
}) {
  return (
    <Row label={label}>
      <NativeSelect value={value} onChange={onChange} className="w-48">
        {/* "아니오"와 "안 적음"은 다르다. 비워 두는 것이 기본값이다. */}
        <option value="">선택하지 않음</option>
        <option value="yes">예</option>
        <option value="no">아니오</option>
      </NativeSelect>
    </Row>
  );
}

function NativeSelect({
  value,
  onChange,
  className,
  children,
}: {
  value: string;
  onChange: (next: string) => void;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={`h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 ${className ?? ""}`}
    >
      {children}
    </select>
  );
}

function toChoice(value: boolean | null | undefined): string {
  if (value === null || value === undefined) return "";
  return value ? "yes" : "no";
}

function fromChoice(value: string): boolean | null {
  if (value === "") return null;
  return value === "yes";
}
