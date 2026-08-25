import type { Mood } from "./mood";

export type Member = {
  id: number;
  username: string;
  nickname: string;
  joinedAt: string;
};

export type Journal = {
  journalDate: string;
  sleepMinutes: number | null;
  studyMinutes: number | null;
  exerciseMinutes: number | null;
  napCount: number | null;
  oneSecondRuleRate: number | null;
  mood: Mood | null;
  slowThinking: boolean | null;
  thoughtBeforeSleep: boolean | null;
  vitaminTaken: boolean | null;
  content: string | null;
};

export type JournalLookup = {
  journalDate: string;
  exists: boolean;
  journal: Journal | null;
};
