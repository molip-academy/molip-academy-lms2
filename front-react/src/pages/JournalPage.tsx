import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError, api } from "@/lib/api";
import { MOODS, MOOD_LABELS } from "@/lib/mood";
import type { Mood } from "@/lib/mood";
import { toHoursAndMinutes, toMinutes } from "@/lib/time";
import { todayIso } from "@/lib/date";
import type { Journal, JournalLookup } from "@/lib/types";
import { AppHeader } from "@/components/AppHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";

type Duration = { hours: string; minutes: string };

const EMPTY_DURATION: Duration = { hours: "", minutes: "" };

/**
 * 폼 전체를 한 덩어리로 다룬다. 필드마다 useState를 두면 응답을 반영할 때 11번,
 * 비울 때 또 11번 setter를 부르게 되고 "사용자가 손댔는지"를 기록할 자리도 없다.
 */
type FormState = {
  sleep: Duration;
  study: Duration;
  exercise: Duration;
  napCount: string;
  rate: string;
  mood: Mood | "";
  slowThinking: string;
  thoughtBeforeSleep: string;
  vitaminTaken: string;
  content: string;
};

const EMPTY_FORM: FormState = {
  sleep: EMPTY_DURATION,
  study: EMPTY_DURATION,
  exercise: EMPTY_DURATION,
  napCount: "",
  rate: "",
  mood: "",
  slowThinking: "",
  thoughtBeforeSleep: "",
  vitaminTaken: "",
  content: "",
};

function toForm(journal: Journal): FormState {
  return {
    sleep: toHoursAndMinutes(journal.sleepMinutes),
    study: toHoursAndMinutes(journal.studyMinutes),
    exercise: toHoursAndMinutes(journal.exerciseMinutes),
    napCount: journal.napCount === null ? "" : String(journal.napCount),
    rate: journal.oneSecondRuleRate === null ? "" : String(journal.oneSecondRuleRate),
    mood: journal.mood ?? "",
    slowThinking: toChoice(journal.slowThinking),
    thoughtBeforeSleep: toChoice(journal.thoughtBeforeSleep),
    vitaminTaken: toChoice(journal.vitaminTaken),
    content: journal.content ?? "",
  };
}

export function JournalPage() {
  const navigate = useNavigate();

  // 날짜는 URL이 소유한다 (ADR 0002와 같은 모양). 그래서 북마크·공유·뒤로가기가 동작한다.
  const { date = todayIso() } = useParams<{ date: string }>();

  // null이면 이 날짜를 아직 한 번도 못 불러온 상태다. 그동안은 스켈레톤을 그린다.
  // 빈 폼을 먼저 보여주면 값이 "기본값에서 갑자기 다른 값으로" 바뀌는 것처럼 보인다.
  const [form, setForm] = useState<FormState | null>(null);
  const [dirty, setDirty] = useState(false);

  const [exists, setExists] = useState(false);
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<{ tone: "ok" | "bad"; text: string } | null>(null);

  /**
   * 요청마다 번호를 매겨 가장 최근 것의 응답만 반영한다. 날짜를 빠르게 바꾸면 이전
   * 날짜의 응답이 늦게 도착해 새 날짜 화면에 옛 데이터를 채워 넣을 수 있다.
   */
  const requestId = useRef(0);

  function update(patch: Partial<FormState>) {
    setDirty(true);
    setForm((current) => ({ ...(current ?? EMPTY_FORM), ...patch }));
  }

  const load = useCallback(async (target: string, announce: boolean) => {
    const id = ++requestId.current;
    setBusy(true);
    setErrors({});
    if (announce) setNotice(null);

    try {
      const result = await api.get<JournalLookup>(`/api/v1/journals/${target}`);
      if (id !== requestId.current) return; // 더 최신 요청이 이미 나갔다

      setExists(result.exists);
      setForm(result.journal ? toForm(result.journal) : EMPTY_FORM);
      setDirty(false);

      if (announce) {
        setNotice({
          tone: "ok",
          text: result.exists ? "일지를 불러왔습니다." : "이 날짜에는 아직 일지가 없습니다.",
        });
      }
    } catch (error) {
      if (id !== requestId.current) return;
      setForm((current) => current ?? EMPTY_FORM); // 스켈레톤에 갇히지 않게
      setNotice({ tone: "bad", text: error instanceof ApiError ? error.message : "불러오지 못했습니다." });
    } finally {
      if (id === requestId.current) setBusy(false);
    }
  }, []);

  // 날짜가 바뀌면 스켈레톤으로 되돌린다. 이전 날짜의 값이 잠깐이라도 남아 있으면 안 된다.
  useEffect(() => {
    setForm(null);
    setDirty(false);
    void load(date, false);
  }, [load, date]);

  /** `조회`는 "다시 불러와라"는 명시적 요청이다. 손댄 게 있을 때만 확인을 받는다. */
  function reload() {
    if (dirty && !window.confirm("저장하지 않은 입력이 있습니다. 불러온 내용으로 바꿀까요?")) return;
    void load(date, true);
  }

  async function save() {
    if (!form) return;
    setBusy(true);
    setErrors({});
    setNotice(null);
    try {
      await api.put(`/api/v1/journals/${date}`, {
        sleepMinutes: toMinutes(form.sleep.hours, form.sleep.minutes),
        studyMinutes: toMinutes(form.study.hours, form.study.minutes),
        exerciseMinutes: toMinutes(form.exercise.hours, form.exercise.minutes),
        napCount: form.napCount === "" ? null : Number(form.napCount),
        oneSecondRuleRate: form.rate === "" ? null : Number(form.rate),
        mood: form.mood === "" ? null : form.mood,
        slowThinking: fromChoice(form.slowThinking),
        thoughtBeforeSleep: fromChoice(form.thoughtBeforeSleep),
        vitaminTaken: fromChoice(form.vitaminTaken),
        content: form.content === "" ? null : form.content,
      });
      setExists(true);
      setDirty(false);
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
      setForm(EMPTY_FORM);
      setDirty(false);
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

      <h1 className="mb-6 text-center text-3xl font-bold tracking-tight sm:mb-8 sm:text-4xl">몰입 일지</h1>

      <div className="rounded-xl border p-4 shadow-sm sm:p-5">
        <div className="mb-6 flex items-center gap-2 sm:gap-3">
          <Input
            type="date"
            value={date}
            onChange={(e) => e.target.value && navigate(`/journals/${e.target.value}`)}
            className="w-auto min-w-0 flex-1"
          />
          <Button onClick={reload} disabled={busy} className="shrink-0 bg-emerald-700 hover:bg-emerald-800">
            조회
          </Button>
        </div>

        {form === null ? (
          <JournalFormSkeleton />
        ) : (
          <>
            <div className="space-y-4">
              <DurationRow
                label="수면시간"
                value={form.sleep}
                onChange={(sleep) => update({ sleep })}
                error={errors.sleepMinutes}
              />
              <DurationRow
                label="공부시간"
                value={form.study}
                onChange={(study) => update({ study })}
                error={errors.studyMinutes}
              />
              <DurationRow
                label="운동시간"
                value={form.exercise}
                onChange={(exercise) => update({ exercise })}
                error={errors.exerciseMinutes}
              />

              {/* 짧은 숫자 두 개가 각각 한 줄씩 차지할 이유가 없다. 좁은 화면에서만 나란히 놓는다. */}
              <div className="grid grid-cols-2 gap-3 sm:block sm:space-y-4">
                <Row label="선잠횟수" unit="회" error={errors.napCount}>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={0}
                    value={form.napCount}
                    onChange={(e) => update({ napCount: e.target.value })}
                    placeholder="횟수"
                    className="w-full min-w-0 sm:w-40"
                  />
                </Row>

                <Row label="1초원칙" unit="%" error={errors.oneSecondRuleRate}>
                  <Input
                    type="number"
                    inputMode="decimal"
                    step="0.1"
                    min={0}
                    max={100}
                    value={form.rate}
                    onChange={(e) => update({ rate: e.target.value })}
                    placeholder="95.5"
                    className="w-full min-w-0 sm:w-40"
                  />
                </Row>
              </div>

              <Row label="기분상태">
                <NativeSelect
                  value={form.mood}
                  onChange={(v) => update({ mood: v as Mood | "" })}
                  className="w-full min-w-0 sm:w-64"
                >
                  <option value="">선택하지 않음</option>
                  {MOODS.map((m) => (
                    <option key={m} value={m}>
                      {MOOD_LABELS[m]}
                    </option>
                  ))}
                </NativeSelect>
              </Row>

              {/* 예/아니오 세 개가 각각 한 줄을 차지하면 폼의 3분의 1이 선택지 세 개로 채워진다. */}
              <div className="grid grid-cols-3 gap-2 sm:block sm:space-y-4">
                <YesNoRow
                  label="슬로싱킹"
                  value={form.slowThinking}
                  onChange={(slowThinking) => update({ slowThinking })}
                />
                <YesNoRow
                  label="취침전생각"
                  value={form.thoughtBeforeSleep}
                  onChange={(thoughtBeforeSleep) => update({ thoughtBeforeSleep })}
                />
                <YesNoRow
                  label="비타민복용"
                  value={form.vitaminTaken}
                  onChange={(vitaminTaken) => update({ vitaminTaken })}
                />
              </div>
            </div>

            <Textarea
              value={form.content}
              onChange={(e) => update({ content: e.target.value })}
              placeholder="오늘의 몰입 기록을 남겨 주세요."
              className="mt-6 min-h-48 sm:min-h-70"
            />
            {errors.content && <p className="mt-2 text-sm text-destructive">{errors.content}</p>}
          </>
        )}
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

      {/*
        저장 버튼은 화면 하단에 붙어 있다. 폼이 한 화면을 넘기 때문에, 아래로 스크롤해야만
        저장할 수 있으면 매일 쓰는 화면에서 그 스크롤이 매번 반복된다.
      */}
      <div className="sticky bottom-0 z-10 -mx-4 mt-5 border-t bg-background/95 px-4 py-3 backdrop-blur sm:static sm:mx-0 sm:border-0 sm:bg-transparent sm:p-0 sm:backdrop-blur-none">
        <Button
          onClick={() => void save()}
          disabled={busy || form === null}
          className="w-full bg-emerald-700 py-6 text-base hover:bg-emerald-800"
        >
          저장하기
        </Button>
      </div>

      {/* 일지가 없는 날에는 지울 것이 없다 */}
      {exists && form !== null && (
        <Button variant="ghost" onClick={() => void remove()} disabled={busy} className="mt-2 w-full text-destructive">
          이 날짜 일지 삭제
        </Button>
      )}
    </div>
  );
}

/** 실제 폼과 같은 높이·배치를 차지한다. 그래야 데이터가 도착해도 화면이 튀지 않는다. */
function JournalFormSkeleton() {
  return (
    <div aria-hidden className="animate-pulse">
      <div className="space-y-4">
        {Array.from({ length: 9 }, (_, i) => (
          <div key={i} className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-4">
            <div className="h-4 w-24 rounded bg-muted sm:w-38 sm:shrink-0" />
            <div className="h-9 w-full rounded bg-muted sm:w-48" />
          </div>
        ))}
      </div>
      <div className="mt-6 min-h-48 rounded-md bg-muted sm:min-h-70" />
    </div>
  );
}

function Row({
  label,
  unit,
  error,
  compact,
  children,
}: {
  label: string;
  unit?: string;
  error?: string;
  /** 3열로 나란히 놓이는 행. 라벨이 좁아 글씨를 줄인다. */
  compact?: boolean;
  children: React.ReactNode;
}) {
  // 좁은 화면에서는 라벨을 입력 위로 쌓는다. 라벨(152px)과 컨트롤을 한 줄에 두면
  // 375px 폰의 카드 안쪽 가용 폭(303px)을 모든 행이 넘긴다.
  return (
    <div className="min-w-0">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-4">
        <div
          className={`font-semibold sm:w-38 sm:shrink-0 sm:text-base ${
            compact ? "truncate text-xs" : "text-sm"
          }`}
        >
          {label}
          {unit && <span className="ml-1 text-xs font-normal text-muted-foreground">({unit})</span>}
        </div>
        <div className="flex w-full min-w-0 items-center gap-2 sm:w-auto">{children}</div>
      </div>
      {error && <p className="mt-1 text-xs text-destructive sm:ml-38 sm:text-sm">{error}</p>}
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
        inputMode="numeric"
        min={0}
        value={value.hours}
        onChange={(e) => onChange({ ...value, hours: e.target.value })}
        placeholder="시"
        className="w-full min-w-0 text-center sm:w-24"
      />
      <span className="text-muted-foreground">:</span>
      <Input
        type="number"
        inputMode="numeric"
        min={0}
        max={59}
        value={value.minutes}
        onChange={(e) => onChange({ ...value, minutes: e.target.value })}
        placeholder="분"
        className="w-full min-w-0 text-center sm:w-24"
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
    <Row label={label} compact>
      <NativeSelect
        value={value}
        onChange={onChange}
        className="w-full min-w-0 px-2 text-xs sm:w-48 sm:px-3 sm:text-sm"
      >
        {/*
          "아니오"와 "안 적음"은 다르다. 비워 두는 것이 기본값이다.
          셋이 한 줄에 놓이면 글자 공간이 74px뿐이라 "선택하지 않음"(76px)이 넘친다.
          "미응답"은 이 프로젝트가 API 문서와 테스트에서 쓰는 용어이기도 하다.
        */}
        <option value="">미응답</option>
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
