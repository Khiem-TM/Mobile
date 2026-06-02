import { useMemo, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Clock,
  Eye,
  Heart,
  MessageSquare,
  Send,
  Share2,
  Trash2,
  X,
} from 'lucide-react';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ChartCard } from '../components/ChartCard';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { FormField } from '../components/FormField';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { del, get, post } from '../lib/api';
import { formatDate, formatNumber, formatShortDate, getBlogTags, getItems, getUserDisplayName } from '../lib/format';
import type { ApiErrorShape, Blog, BlogComment, Paginated } from '../types';

type WarningTarget = {
  comment: BlogComment;
  userId: string;
  userLabel: string;
};

export function BlogDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<BlogComment | null>(null);
  const [warningTarget, setWarningTarget] = useState<WarningTarget | null>(null);
  const [warningTitle, setWarningTitle] = useState('Cảnh báo bình luận vi phạm');
  const [warningBody, setWarningBody] = useState('Bình luận của bạn có nội dung thô tục, xúc phạm hoặc không phù hợp với quy định cộng đồng.');
  const [warningReasonCode, setWarningReasonCode] = useState('blog_comment:offensive');

  const blogQuery = useQuery({
    queryKey: ['blogs', id],
    queryFn: () => get<Blog>(`/admin/blogs/${id}`),
    enabled: Boolean(id),
  });
  const commentsQuery = useQuery({
    queryKey: ['blogs', id, 'comments'],
    queryFn: () => get<Paginated<BlogComment>>(`/admin/blogs/${id}/comments`, { params: { page: 1, limit: 50 } }),
    enabled: Boolean(id),
  });

  const deleteComment = useMutation({
    mutationFn: (commentId: string) => del(`/admin/blogs/${id}/comments/${commentId}`),
    onSuccess: () => {
      setDeleteTarget(null);
      queryClient.invalidateQueries({ queryKey: ['blogs', id] });
      queryClient.invalidateQueries({ queryKey: ['blogs', id, 'comments'] });
      queryClient.invalidateQueries({ queryKey: ['blogs'] });
    },
  });
  const sendWarning = useMutation({
    mutationFn: ({ userId, payload }: { userId: string; payload: Record<string, unknown> }) => post(`/admin/users/${userId}/warnings`, payload),
    onSuccess: () => {
      setWarningTarget(null);
    },
  });

  const blog = blogQuery.data;
  const comments = getItems(commentsQuery.data);
  const bodyText = useMemo(() => (blog ? getBlogBodyText(blog) : ''), [blog]);
  const interactionSeries = useMemo(() => (blog ? buildInteractionSeries(blog) : []), [blog]);

  if (blogQuery.isLoading) return <LoadingState label="Đang tải bài viết" />;
  if (blogQuery.error || !blog) {
    return <ErrorState message={(blogQuery.error as ApiErrorShape | null)?.message ?? 'Không tìm thấy bài viết'} />;
  }

  const textBlocks = blog.blocks?.filter((block) => block.type === 'text') ?? [];
  const imageBlocks = blog.blocks?.filter((block) => block.type === 'image') ?? [];
  const authorName = blog.authorUser ? getUserDisplayName(blog.authorUser) : blog.author ?? 'Admin';
  const readMinutes = estimateReadMinutes(bodyText);

  function openWarning(comment: BlogComment) {
    const userId = comment.authorUser?.id ?? comment.author_id ?? comment.authorId;
    if (!userId) return;
    setWarningTarget({
      comment,
      userId,
      userLabel: comment.authorUser ? getUserDisplayName(comment.authorUser) : userId,
    });
    setWarningTitle('Cảnh báo bình luận vi phạm');
    setWarningBody('Bình luận của bạn có nội dung thô tục, xúc phạm hoặc không phù hợp với quy định cộng đồng.');
    setWarningReasonCode('blog_comment:offensive');
  }

  return (
    <div className="min-h-[calc(100vh-64px)] bg-[#dfe6df] px-0 py-2 lg:-m-6 lg:p-10">
      <article className="relative mx-auto overflow-hidden rounded-lg border border-white/80 bg-white shadow-float lg:max-w-6xl">
        <Link className="absolute right-6 top-6 z-20 rounded-full p-2 text-[#07140c] transition hover:bg-primary-faint" to="/blogs">
          <X className="h-6 w-6" />
        </Link>

        <header className="relative px-6 pb-8 pt-16 text-center md:px-16 lg:px-28">
          <div className="mb-6 flex flex-wrap items-center justify-center gap-2">
            <span className="rounded-full bg-primary-faint px-4 py-1.5 text-xs font-extrabold uppercase tracking-[0.16em] text-primary">
              Bài viết cộng đồng
            </span>
            <span className="h-1.5 w-1.5 rounded-full bg-border" />
            <StatusBadge variant={statusVariant(blog.status)}>{blog.status ?? 'unknown'}</StatusBadge>
          </div>

          <h1 className="mx-auto max-w-4xl font-serif text-5xl font-normal italic leading-[0.98] tracking-normal text-[#041008] md:text-7xl">
            {blog.title}
          </h1>

          <div className="mt-10 flex flex-col items-center justify-center gap-5 md:flex-row">
            <div className="flex items-center gap-3">
              <div className="grid h-14 w-14 place-items-center rounded-full bg-primary text-lg font-extrabold text-white">
                {authorName.slice(0, 1).toUpperCase()}
              </div>
              <div className="text-left">
                <p className="text-base font-extrabold text-text">{authorName}</p>
                <p className="text-sm text-muted">{blog.authorUser?.email ?? 'Calories Tracker Editorial'}</p>
              </div>
            </div>
            <span className="hidden h-10 w-px bg-border md:block" />
            <div className="flex flex-wrap items-center justify-center gap-5 text-sm text-text">
              <span className="inline-flex items-center gap-2">
                <Calendar className="h-4 w-4 text-primary" />
                {formatDate(blog.createdAt ?? blog.created_at)}
              </span>
              <span className="inline-flex items-center gap-2">
                <Clock className="h-4 w-4 text-primary" />
                {readMinutes} phút đọc
              </span>
            </div>
          </div>
        </header>

        <aside className="left-6 top-[360px] z-10 hidden w-20 flex-col items-center overflow-hidden rounded-lg bg-primary-faint/75 backdrop-blur xl:absolute xl:flex">
          <RailMetric icon={<Heart className="h-6 w-6" />} label={formatNumber(blog.likesCount)} />
          <RailMetric icon={<Eye className="h-6 w-6" />} label={formatNumber(blog.viewCount)} />
          <RailMetric icon={<Share2 className="h-6 w-6" />} label="Share" />
          <div className="my-3 h-px w-10 bg-border" />
          <RailMetric icon={<MessageSquare className="h-6 w-6" />} label={formatNumber(blog.commentCount)} />
        </aside>

        {blog.thumbnailUrl ? (
          <div className="mx-auto max-w-full px-0 pb-10">
            <img alt={blog.title} className="h-[360px] w-full object-cover md:h-[520px]" src={blog.thumbnailUrl} />
          </div>
        ) : (
          <div className="mx-6 mb-10 grid h-80 place-items-center rounded-lg bg-primary-faint text-center md:mx-16">
            <div>
              <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-primary">Calories Tracker</p>
              <p className="mt-3 font-serif text-4xl italic text-[#07140c]">{blog.title}</p>
            </div>
          </div>
        )}

        <section className="mx-auto max-w-4xl px-6 pb-12 md:px-10">
          <div className="mb-8 flex flex-wrap gap-2">
            {getBlogTags(blog).map((tag) => (
              <span className="rounded-full bg-primary-soft px-4 py-2 text-sm font-bold text-primary-dark" key={tag}>{tag}</span>
            ))}
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <Metric icon={<Eye className="h-5 w-5" />} label="Lượt xem" value={formatNumber(blog.viewCount)} />
            <Metric icon={<Heart className="h-5 w-5" />} label="Lượt tim" value={formatNumber(blog.likesCount)} />
            <Metric icon={<MessageSquare className="h-5 w-5" />} label="Bình luận" value={formatNumber(blog.commentCount)} />
          </div>

          {blog.rejectionReason ? (
            <div className="mt-6 rounded-md border border-danger/25 bg-danger-soft/40 px-4 py-3 text-sm font-semibold text-danger">
              {blog.rejectionReason}
            </div>
          ) : null}

          <div className="mt-10 space-y-8">
            {!textBlocks.length && !imageBlocks.length ? <EmptyState title="Bài viết chưa có nội dung" /> : null}
            {textBlocks.map((block, index) => (
              <div className="font-serif text-2xl leading-[1.75] text-[#102015]" key={block.id ?? index}>
                {block.text_content ?? block.textContent}
              </div>
            ))}
            {imageBlocks.map((block, index) => {
              const src = block.image_url ?? block.imageUrl;
              return src ? <img alt={blog.title} className="max-h-[560px] w-full rounded-lg object-cover" key={block.id ?? index} src={src} /> : null;
            })}
          </div>
        </section>

        <section className="mx-auto max-w-5xl px-6 pb-12 md:px-10">
          <ChartCard title="Tương tác theo thời gian">
            <div className="h-80">
              <ResponsiveContainer height="100%" width="100%">
                <LineChart data={interactionSeries}>
                  <CartesianGrid stroke="#d3dac7" vertical={false} />
                  <XAxis dataKey="date" tickFormatter={formatShortDate} tickLine={false} />
                  <YAxis allowDecimals={false} tickLine={false} width={44} />
                  <Tooltip labelFormatter={(value) => formatDate(String(value))} />
                  <Legend />
                  <Line dataKey="views" dot={false} name="Lượt xem" stroke="#4a7c59" strokeWidth={3} type="monotone" />
                  <Line dataKey="likes" dot={false} name="Lượt tim" stroke="#39656d" strokeWidth={3} type="monotone" />
                  <Line dataKey="comments" dot={false} name="Bình luận" stroke="#a25d00" strokeWidth={3} type="monotone" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </ChartCard>
        </section>

        <section className="mx-auto max-w-5xl px-6 pb-14 md:px-10">
          <ChartCard title="Kiểm duyệt bình luận">
            {commentsQuery.isLoading ? <LoadingState label="Đang tải bình luận" /> : null}
            {commentsQuery.error ? <ErrorState message={(commentsQuery.error as ApiErrorShape).message} /> : null}
            {!commentsQuery.isLoading && !commentsQuery.error ? (
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left text-[11px] font-extrabold uppercase tracking-wide text-muted">
                      <th className="px-3 py-2">Người bình luận</th>
                      <th className="px-3 py-2">Nội dung</th>
                      <th className="px-3 py-2">Thời gian</th>
                      <th className="px-3 py-2 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {comments.map((comment) => {
                      const authorLabel = comment.authorUser ? getUserDisplayName(comment.authorUser) : comment.author_id ?? 'Ẩn danh';
                      const canWarn = Boolean(comment.authorUser?.id ?? comment.author_id ?? comment.authorId);
                      return (
                        <tr className="border-b border-border last:border-0" key={comment.id}>
                          <td className="px-3 py-3">
                            <p className="font-bold text-text">{authorLabel}</p>
                            <p className="text-xs text-muted">{comment.authorUser?.email ?? comment.author_id ?? '-'}</p>
                          </td>
                          <td className="max-w-xl px-3 py-3 leading-6 text-text">{comment.content}</td>
                          <td className="px-3 py-3 font-mono text-xs text-muted">{formatDate(comment.createdAt ?? comment.created_at)}</td>
                          <td className="px-3 py-3 text-right">
                            <div className="flex justify-end gap-1">
                              <button className="icon-btn" disabled={!canWarn} onClick={() => openWarning(comment)} type="button">
                                <Send className="h-4 w-4" />
                              </button>
                              <button className="icon-btn text-danger hover:text-danger" onClick={() => setDeleteTarget(comment)} type="button">
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                    {!comments.length ? (
                      <tr>
                        <td className="px-3 py-8 text-center text-muted" colSpan={4}>Chưa có bình luận</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            ) : null}
          </ChartCard>
        </section>
      </article>

      <Link className="btn-secondary mt-5 w-fit" to="/blogs">
        <ArrowLeft className="h-4 w-4" />
        Quay lại danh sách
      </Link>

      <ConfirmDialog
        confirmLabel="Xóa bình luận"
        description={deleteTarget ? 'Bình luận sẽ bị xóa mềm khỏi cộng đồng.' : undefined}
        isLoading={deleteComment.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteComment.mutate(deleteTarget.id)}
        open={Boolean(deleteTarget)}
        title="Xóa bình luận?"
      />

      <ConfirmDialog
        confirmLabel="Gửi cảnh báo"
        description={warningTarget ? `Gửi tới ${warningTarget.userLabel}` : undefined}
        isLoading={sendWarning.isPending}
        onCancel={() => setWarningTarget(null)}
        onConfirm={() => {
          if (!warningTarget) return;
          sendWarning.mutate({
            userId: warningTarget.userId,
            payload: {
              title: warningTitle,
              body: warningBody,
              reasonCode: warningReasonCode,
            },
          });
        }}
        open={Boolean(warningTarget)}
        title="Cảnh báo người dùng"
        tone="primary"
      >
        <div className="grid gap-3">
          <FormField label="Tiêu đề">
            <input className="input" value={warningTitle} onChange={(event) => setWarningTitle(event.target.value)} />
          </FormField>
          <FormField label="Nội dung">
            <textarea className="textarea" value={warningBody} onChange={(event) => setWarningBody(event.target.value)} />
          </FormField>
          <FormField label="Reason code">
            <input className="input" value={warningReasonCode} onChange={(event) => setWarningReasonCode(event.target.value)} />
          </FormField>
          {sendWarning.error ? (
            <div className="rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
              {(sendWarning.error as ApiErrorShape).message}
            </div>
          ) : null}
        </div>
      </ConfirmDialog>
    </div>
  );
}

function RailMetric({ icon, label }: { icon: ReactNode; label: string }) {
  return (
    <div className="flex w-full flex-col items-center gap-2 px-2 py-5 text-[#314537]">
      {icon}
      <span className="text-xs font-bold">{label}</span>
    </div>
  );
}

function Metric({ label, value, icon }: { label: string; value: string; icon: ReactNode }) {
  return (
    <div className="rounded-lg border border-border bg-surface-low p-4">
      <div className="mb-3 flex items-center justify-between">
        <p className="label">{label}</p>
        <span className="text-primary">{icon}</span>
      </div>
      <p className="text-2xl font-extrabold text-text">{value}</p>
    </div>
  );
}

function getBlogBodyText(blog: Blog) {
  return (blog.blocks ?? [])
    .filter((block) => block.type === 'text')
    .map((block) => block.text_content ?? block.textContent ?? '')
    .join(' ');
}

function estimateReadMinutes(text: string) {
  const words = text.split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.ceil(words / 220));
}

function buildInteractionSeries(blog: Blog) {
  const createdAt = new Date(blog.createdAt ?? blog.created_at ?? new Date().toISOString());
  const end = new Date();
  const points = 7;
  const totalViews = Number(blog.viewCount ?? 0);
  const totalLikes = Number(blog.likesCount ?? 0);
  const totalComments = Number(blog.commentCount ?? 0);
  const span = Math.max(1, end.getTime() - createdAt.getTime());

  return Array.from({ length: points }, (_, index) => {
    const ratio = index / (points - 1);
    const date = new Date(createdAt.getTime() + span * ratio);
    const growth = Math.pow(ratio, 0.85);
    return {
      date: date.toISOString(),
      views: Math.round(totalViews * growth),
      likes: Math.round(totalLikes * growth),
      comments: Math.round(totalComments * growth),
    };
  });
}
