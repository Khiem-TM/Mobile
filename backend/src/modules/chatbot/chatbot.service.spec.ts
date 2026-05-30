import { Readable } from 'stream';
import axios from 'axios';
import { ChatbotService } from './chatbot.service';

jest.mock('axios');

const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('ChatbotService streaming', () => {
  const sessionId = '00000000-0000-0000-0000-000000000001';
  const userId = '00000000-0000-0000-0000-000000000002';

  let savedMessages: any[];
  let sessionRepo: any;
  let messageRepo: any;
  let service: ChatbotService;

  beforeEach(() => {
    savedMessages = [];
    jest.clearAllMocks();

    sessionRepo = {
      findOne: jest.fn().mockResolvedValue({ id: sessionId, user_id: userId }),
      update: jest.fn().mockResolvedValue(undefined),
    };
    messageRepo = {
      find: jest.fn().mockResolvedValue([]),
      create: jest.fn((payload) => payload),
      save: jest.fn(async (payload) => {
        const saved = {
          id: `msg_${savedMessages.length + 1}`,
          created_at: new Date('2026-05-30T00:00:00.000Z'),
          ...payload,
        };
        savedMessages.push(saved);
        return saved;
      }),
    };
    const aiContextService = {
      buildUserContext: jest.fn().mockResolvedValue({ schema_version: '1.0', user_ref: 'u_test' }),
    };

    service = new ChatbotService(sessionRepo, messageRepo, aiContextService as any);
  });

  async function collect() {
    const events = [];
    for await (const event of service.streamMessage(userId, sessionId, 'Xin chào')) {
      events.push(event);
    }
    return events;
  }

  it('proxies upstream SSE, aggregates reply, and persists the exchange', async () => {
    mockedAxios.post.mockResolvedValue({
      data: Readable.from([
        'event: meta\n',
        'data: {"intent":"nutrition","sources":[{"title":"Macro","document_id":"doc1","chunk_index":0}]}\n\n',
        'event: delta\n',
        'data: {"text":"Xin "}\n\n',
        'event: delta\n',
        'data: {"text":"chào"}\n\n',
        'event: done\n',
        'data: {"intent":"nutrition","disclaimer":"Tham khảo"}\n\n',
      ]),
    });

    const events = await collect();

    expect(events.map((event) => event.event)).toEqual(['meta', 'delta', 'delta', 'done']);
    expect(savedMessages).toHaveLength(2);
    expect(savedMessages[0]).toMatchObject({ role: 'user', content: 'Xin chào' });
    expect(savedMessages[1]).toMatchObject({ role: 'assistant', content: 'Xin chào' });
    expect(sessionRepo.update).toHaveBeenCalledWith(
      sessionId,
      expect.objectContaining({ last_message: 'Xin chào', title: 'Xin chào' }),
    );
  });

  it('persists fallback when upstream fails before any token', async () => {
    mockedAxios.post.mockRejectedValue(new Error('rag unavailable'));

    const events = await collect();

    expect(events.map((event) => event.event)).toEqual(['error', 'done']);
    expect(savedMessages).toHaveLength(2);
    expect(savedMessages[1].role).toBe('assistant');
    expect(savedMessages[1].content).toContain('trợ lý đang bận');
  });

  it('falls back to sync chat when only streaming fails before any token', async () => {
    mockedAxios.post
      .mockRejectedValueOnce(new Error('stream unavailable'))
      .mockResolvedValueOnce({ data: { reply: 'Sync reply' } });

    const events = await collect();

    expect(events.map((event) => event.event)).toEqual(['delta', 'done']);
    expect((events[0] as any).data.text).toBe('Sync reply');
    expect(savedMessages).toHaveLength(2);
    expect(savedMessages[1]).toMatchObject({ role: 'assistant', content: 'Sync reply' });
  });

  it('does not persist a partial assistant message when stream fails after a token', async () => {
    mockedAxios.post.mockResolvedValue({
      data: Readable.from([
        'event: delta\n',
        'data: {"text":"Một phần"}\n\n',
        'event: error\n',
        'data: {"message":"upstream failed"}\n\n',
      ]),
    });

    const events = await collect();

    expect(events.map((event) => event.event)).toEqual(['delta', 'error']);
    expect(savedMessages).toHaveLength(0);
  });
});
