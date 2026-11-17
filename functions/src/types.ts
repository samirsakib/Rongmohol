export type CardColor = "RED" | "BLUE" | "GREEN" | "YELLOW" | "WILD";
export type ActiveSuitColor = "RED" | "BLUE" | "GREEN" | "YELLOW";

export type CardType =
  | "NUMBER"
  | "SKIP"
  | "REVERSE"
  | "DRAW_TWO"
  | "WILD"
  | "WILD_DRAW_FOUR"
  | "THUNDERBOLT_PLUS_THREE"
  | "HAND_SWAP"
  | "BOUNCE_BACK"
  | "EVERYONE_DRAW_ONE"
  | "MYSTERY";

export type DeckType = "CLASSIC" | "SPECIAL";
export type RoomStatus = "waiting" | "playing" | "finished";

export interface GameCard {
  id: string;
  color: CardColor;
  type: CardType;
  number: number | null;
  pointValue: number;
}

export interface RoomMeta {
  roomId: string;
  roomCode: string;
  title: string;
  hostUid: string;
  hostName: string;
  status: RoomStatus;
  deckType: DeckType;
  maxPlayers: number;
  isPrivate: boolean;
  startingCards: number;
  turnTimeoutSeconds: number;
  createdAt: number;
  updatedAt: number;
}

export interface RoomPlayer {
  uid: string;
  displayName: string;
  avatarId: number;
  seatIndex: number;
  ready: boolean;
  connected: boolean;
  lastSeenAt: number;
}

export interface MatchEventSummary {
  type: string;
  actorUid: string;
  actorName: string;
  targetUid?: string;
  cardType?: CardType;
  resolvedColor?: ActiveSuitColor;
  count?: number;
  messageBengali: string;
  timestamp: number;
}

export interface PlayerMatchResult {
  uid: string;
  displayName: string;
  avatarId: number;
  cardsRemaining: number;
  handPointsRemaining: number;
  awardedPoints: number;
  isWinner: boolean;
}

export interface PublicGameState {
  version: number;
  deckType: DeckType;
  topDiscardCard: GameCard;
  activeColor: ActiveSuitColor;
  drawPileCount: number;
  discardPileCount: number;
  pendingDrawPenalty: number;
  direction: 1 | -1;
  currentTurnIndex: number;
  currentTurnUid: string;
  turnDeadlineAt: number;
  turnTimeoutSeconds: number;
  seatOrder: string[];
  playerCardCounts: Record<string, number>;
  declaredLastCard: Record<string, boolean>;
  lastEvent: MatchEventSummary;
  winnerUid: string | null;
  results: Record<string, PlayerMatchResult> | null;
}

export interface ServerPrivateState {
  drawPile: GameCard[];
  discardPile: GameCard[];
  hands: Record<string, GameCard[]>;
  processedActionIds: Record<string, number>;
  lastActorUid: string | null;
}

export interface AuthoritativeRoomNode {
  meta: RoomMeta;
  players: Record<string, RoomPlayer>;
  publicState?: PublicGameState;
  playerHands?: Record<string, GameCard[]>;
  serverPrivate?: ServerPrivateState;
}

export interface PlayCardCommandInput {
  roomId: string;
  actionId: string;
  expectedVersion?: number;
  command: "PLAY_CARD" | "DRAW_CARD" | "DECLARE_LAST_CARD";
  cardId?: string;
  chosenColor?: ActiveSuitColor;
  targetPlayerId?: string;
}
