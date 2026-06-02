import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Bold,
  Camera,
  Check,
  Edit,
  Eye,
  Heading1,
  ImagePlus,
  Link as LinkIcon,
  List,
  PenLine,
  Plus,
  Quote,
  RotateCcw,
  Trash2,
  X,
} from 'lucide-react';
import { z } from 'zod';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataTable, type DataColumn } from '../components/DataTable';
import { FilterBar } from '../components/FilterBar';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { compactParams, del, get, patch, post } from '../lib/api';
import { formatDate, formatNumber, getBlogTags, getItems } from '../lib/format';
import type { ApiErrorShape, Blog, BlogStatus, Paginated } from '../types';

const blogSchema = z.object({
  title: z.string().min(1, 'Vui lòng nhập tiêu đề'),
  thumbnailUrl: z.string().url('Thumbnail URL không hợp lệ').or(z.literal('')).optional(),
  thumbnailBase64: z.string().optional(),
  tags: z.array(z.string()).optional(),
  status: z.enum(['draft', 'approved']).optional(),
  blocks: z
    .array(
      z.object({
        order: z.number().min(0),
        type: z.enum(['text', 'image']),
        text_content: z.string().optional(),
        image_url: z.string().url().optional(),
      }),
    )
    .optional(),
});

type BlogFilters = {
  search: string;
  status: string;
  tag: string;
  authorId: string;
  createdFrom: string;
  createdTo: string;
};

const initialFilters: BlogFilters = {
  search: '',
  status: '',
  tag: '',
  authorId: '',
  createdFrom: '',
  createdTo: '',
};

export function BlogsPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState<{ mode: 'create' | 'edit'; blog?: Blog } | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Blog | null>(null);
  const [rejectState, setRejectState] = useState<{ ids: string[]; title: string } | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [formError, setFormError] = useState('');

  const params = useMemo(() => compactParams({ page, limit: 20, ...filters }), [filters, page]);
  const blogsQuery = useQuery({
    queryKey: ['blogs', params],
    queryFn: () => get<Paginated<Blog>>('/admin/blogs', { params }),
  });

  const createBlog = useMutation({
    mutationFn: (payload: Record<string, unknown>) => post<Blog>('/admin/blogs', payload),
    onSuccess: () => closeAndRefresh(),
  });
  const updateBlog = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => patch<Blog>(`/admin/blogs/${id}`, payload),
    onSuccess: () => closeAndRefresh(),
  });
  const approveBlog = useMutation({
    mutationFn: (id: string) => patch<Blog>(`/admin/blogs/${id}/approve`),
    onSuccess: () => refreshBlogs(),
  });
  const rejectBlog = useMutation({
    mutationFn: ({ ids, reason }: { ids: string[]; reason?: string }) =>
      ids.length === 1
        ? patch<Blog>(`/admin/blogs/${ids[0]}/reject`, { reason })
        : post('/admin/blogs/batch/reject', { ids, reason }),
    onSuccess: () => {
      setRejectState(null);
      setRejectReason('');
      setSelectedIds([]);
      refreshBlogs();
    },
  });
  const batchApprove = useMutation({
    mutationFn: (ids: string[]) => post('/admin/blogs/batch/approve', { ids }),
    onSuccess: () => {
      setSelectedIds([]);
      refreshBlogs();
    },
  });
  const deleteBlog = useMutation({
    mutationFn: (id: string) => del(`/admin/blogs/${id}`),
    onSuccess: () => {
      setDeleteTarget(null);
      refreshBlogs();
    },
  });

  function refreshBlogs() {
    queryClient.invalidateQueries({ queryKey: ['blogs'] });
    queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
  }

  function closeAndRefresh() {
    setDrawer(null);
    setFormError('');
    refreshBlogs();
  }

  async function submitBlogForm(formData: FormData) {
    const text = String(formData.get('text') ?? '').trim();
    const imageUrl = String(formData.get('imageUrl') ?? '').trim();
    const thumbnailFile = formData.get('thumbnailFile');
    const thumbnailBase64 =
      thumbnailFile instanceof File && thumbnailFile.size > 0
        ? await fileToBase64(thumbnailFile)
        : undefined;
    const blocks = [
      text ? { order: 0, type: 'text' as const, text_content: text } : null,
      imageUrl ? { order: text ? 1 : 0, type: 'image' as const, image_url: imageUrl } : null,
    ].filter(Boolean);
    const raw = {
      title: String(formData.get('title') ?? '').trim(),
      thumbnailUrl: String(formData.get('thumbnailUrl') ?? '').trim(),
      thumbnailBase64,
      tags: String(formData.get('tags') ?? '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
      status: String(formData.get('status') ?? 'approved') as 'draft' | 'approved',
      blocks: blocks.length ? blocks : undefined,
    };
    const parsed = blogSchema.safeParse(raw);
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu blog không hợp lệ');
      return;
    }
    const payload = Object.fromEntries(
      Object.entries(parsed.data).filter(([, value]) => value !== '' && value !== undefined && !(Array.isArray(value) && !value.length)),
    );
    if (drawer?.mode === 'edit' && drawer.blog) updateBlog.mutate({ id: drawer.blog.id, payload });
    else createBlog.mutate(payload);
  }

  const currentRows = getItems(blogsQuery.data);
  const allSelected = currentRows.length > 0 && currentRows.every((blog) => selectedIds.includes(blog.id));

  const columns: DataColumn<Blog>[] = [
    {
      key: 'select',
      header: '',
      width: '44px',
      render: (blog) => (
        <input
          checked={selectedIds.includes(blog.id)}
          className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
          onChange={(event) => {
            setSelectedIds((ids) => (event.target.checked ? [...ids, blog.id] : ids.filter((id) => id !== blog.id)));
          }}
          type="checkbox"
        />
      ),
    },
    {
      key: 'title',
      header: 'Bài viết',
      render: (blog) => (
        <div className="min-w-[220px]">
          <Link className="font-extrabold text-text hover:text-primary" to={`/blogs/${blog.id}`}>{blog.title}</Link>
          <p className="truncate text-xs text-muted">{getBlogTags(blog).join(', ') || 'Không tag'}</p>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Trạng thái',
      render: (blog) => <StatusBadge variant={statusVariant(blog.status)}>{labelBlogStatus(blog.status)}</StatusBadge>,
    },
    { key: 'author', header: 'Tác giả', render: (blog) => <span>{blog.author ?? blog.author_id ?? blog.authorId ?? 'Admin'}</span> },
    {
      key: 'metrics',
      header: 'Tương tác',
      render: (blog) => (
        <span className="font-mono text-xs">{formatNumber(blog.viewCount)} views · {formatNumber(blog.likesCount)} likes · {formatNumber(blog.commentCount)} cmt</span>
      ),
    },
    { key: 'created', header: 'Ngày tạo', render: (blog) => <span className="font-mono text-xs">{formatDate(blog.createdAt ?? blog.created_at)}</span> },
    {
      key: 'actions',
      header: 'Thao tác',
      className: 'text-right',
      render: (blog) => (
        <div className="flex justify-end gap-1">
          <Link className="icon-btn" to={`/blogs/${blog.id}`}><Eye className="h-4 w-4" /></Link>
          <button className="icon-btn" onClick={() => setDrawer({ mode: 'edit', blog })} type="button"><Edit className="h-4 w-4" /></button>
          {blog.status !== 'approved' ? (
            <button className="icon-btn text-primary hover:text-primary" onClick={() => approveBlog.mutate(blog.id)} type="button"><Check className="h-4 w-4" /></button>
          ) : null}
          {blog.status !== 'rejected' ? (
            <button className="icon-btn text-danger hover:text-danger" onClick={() => setRejectState({ ids: [blog.id], title: `Từ chối: ${blog.title}` })} type="button"><X className="h-4 w-4" /></button>
          ) : null}
          <button className="icon-btn text-danger hover:text-danger" onClick={() => setDeleteTarget(blog)} type="button"><Trash2 className="h-4 w-4" /></button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="page-title">Blog & CMS</h1>
          <p className="mt-1 text-sm text-muted">Duyệt, từ chối, chỉnh sửa và xóa mềm bài viết</p>
        </div>
        <button className="btn-primary" onClick={() => setDrawer({ mode: 'create' })} type="button">
          <Plus className="h-4 w-4" />
          Tạo bài viết
        </button>
      </div>

      <FilterBar
        actions={
          <>
            <button className="btn-secondary" onClick={() => setFilters(initialFilters)} type="button">
              <RotateCcw className="h-4 w-4" />
              Đặt lại
            </button>
            <button className="btn-secondary" disabled={!selectedIds.length} onClick={() => batchApprove.mutate(selectedIds)} type="button">
              Duyệt batch
            </button>
            <button className="btn-secondary" disabled={!selectedIds.length} onClick={() => setRejectState({ ids: selectedIds, title: `Từ chối ${selectedIds.length} bài viết` })} type="button">
              Từ chối batch
            </button>
          </>
        }
      >
        <input className="input" placeholder="Tìm tiêu đề..." value={filters.search} onChange={(event) => setFilters({ ...filters, search: event.target.value })} />
        <select className="select" value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
          <option value="">Tất cả trạng thái</option>
          <option value="pending">Pending</option>
          <option value="approved">Approved</option>
          <option value="rejected">Rejected</option>
          <option value="draft">Draft</option>
        </select>
        <input className="input" placeholder="Tag" value={filters.tag} onChange={(event) => setFilters({ ...filters, tag: event.target.value })} />
        <input className="input" placeholder="Author ID" value={filters.authorId} onChange={(event) => setFilters({ ...filters, authorId: event.target.value })} />
        <input className="input" type="date" value={filters.createdFrom} onChange={(event) => setFilters({ ...filters, createdFrom: event.target.value })} />
      </FilterBar>

      <div className="mb-2 flex items-center gap-2 text-sm text-muted">
        <input
          checked={allSelected}
          className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
          onChange={(event) => setSelectedIds(event.target.checked ? currentRows.map((blog) => blog.id) : [])}
          type="checkbox"
        />
        Chọn tất cả trang hiện tại
      </div>

      <DataTable
        columns={columns}
        data={currentRows}
        emptyTitle="Không có bài viết phù hợp"
        error={(blogsQuery.error as ApiErrorShape | null)?.message}
        getRowId={(blog) => blog.id}
        isLoading={blogsQuery.isLoading}
        limit={blogsQuery.data?.limit}
        onPageChange={setPage}
        page={blogsQuery.data?.page ?? page}
        total={blogsQuery.data?.total}
      />

      <BlogEditorModal
        blog={drawer?.blog}
        error={formError || (createBlog.error as ApiErrorShape | null)?.message || (updateBlog.error as ApiErrorShape | null)?.message}
        isSubmitting={createBlog.isPending || updateBlog.isPending}
        mode={drawer?.mode ?? 'create'}
        onClose={() => setDrawer(null)}
        open={Boolean(drawer)}
        onSubmit={submitBlogForm}
      />

      <ConfirmDialog
        confirmLabel="Từ chối"
        description="Lý do sẽ được lưu vào trạng thái moderation."
        isLoading={rejectBlog.isPending}
        onCancel={() => {
          setRejectState(null);
          setRejectReason('');
        }}
        onConfirm={() => rejectState && rejectBlog.mutate({ ids: rejectState.ids, reason: rejectReason || undefined })}
        open={Boolean(rejectState)}
        title={rejectState?.title ?? 'Từ chối bài viết'}
      >
        <textarea className="textarea" value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} placeholder="Lý do từ chối" />
      </ConfirmDialog>

      <ConfirmDialog
        confirmLabel="Xóa mềm"
        description={deleteTarget ? `${deleteTarget.title} sẽ bị ẩn khỏi public API.` : undefined}
        isLoading={deleteBlog.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteBlog.mutate(deleteTarget.id)}
        open={Boolean(deleteTarget)}
        title="Xóa mềm bài viết?"
      />
    </div>
  );
}

function BlogEditorModal({
  open,
  mode,
  blog,
  isSubmitting,
  error,
  onClose,
  onSubmit,
}: {
  open: boolean;
  mode: 'create' | 'edit';
  blog?: Blog;
  isSubmitting: boolean;
  error?: string;
  onClose: () => void;
  onSubmit: (formData: FormData) => void | Promise<void>;
}) {
  const textBlock = blog?.blocks?.find((block) => block.type === 'text');
  const imageBlock = blog?.blocks?.find((block) => block.type === 'image');
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [preview, setPreview] = useState(blog?.thumbnailUrl ?? '');
  const [tags, setTags] = useState<string[]>(blog ? getBlogTags(blog) : ['Dinh dưỡng']);
  const [status, setStatus] = useState<'approved' | 'draft'>(blog?.status === 'draft' ? 'draft' : 'approved');
  const tagOptions = ['Dinh dưỡng', 'Giảm cân', 'Thể thao', 'Sức khỏe', 'AI Scan'];

  useEffect(() => {
    if (!open) return;
    setPreview(blog?.thumbnailUrl ?? '');
    setTags(blog ? getBlogTags(blog) : ['Dinh dưỡng']);
    setStatus(blog?.status === 'draft' ? 'draft' : 'approved');
  }, [blog, open]);

  if (!open) return null;

  function toggleTag(tag: string) {
    setTags((current) => (current.includes(tag) ? current.filter((item) => item !== tag) : [...current, tag]));
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-[#6f8f79]/55 px-4 py-8 backdrop-blur-sm">
      <form
        className="mx-auto flex min-h-[min(920px,92vh)] w-full max-w-6xl flex-col overflow-hidden rounded-lg border border-white/70 bg-white shadow-float"
        onSubmit={(event) => {
          event.preventDefault();
          const formData = new FormData(event.currentTarget);
          const submitter = (event.nativeEvent as SubmitEvent).submitter;
          if (submitter instanceof HTMLButtonElement && submitter.name) {
            formData.set(submitter.name, submitter.value);
          }
          onSubmit(formData);
        }}
      >
        <header className="flex min-h-24 items-center justify-between border-b border-border/60 px-8 md:px-10">
          <div className="flex items-center gap-3">
            <PenLine className="h-5 w-5 text-primary" />
            <h2 className="font-serif text-2xl font-extrabold tracking-normal text-[#07140c]">
              {mode === 'edit' ? 'Chỉnh sửa bài viết' : 'Đăng bài viết cộng đồng'}
            </h2>
          </div>
          <button className="icon-btn border-0 text-text" onClick={onClose} type="button">
            <X className="h-6 w-6" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-8 py-10 md:px-20">
          <input name="tags" type="hidden" value={tags.join(', ')} />
          <input name="status" type="hidden" value={status} />
          <input
            name="thumbnailUrl"
            type="hidden"
            value={preview && !preview.startsWith('data:') && !preview.startsWith('blob:') ? preview : ''}
          />

          <label className="block">
            <span className="mb-2 block text-xs font-extrabold uppercase tracking-[0.16em] text-primary">Tiêu đề bài viết</span>
            <input
              className="w-full border-0 bg-transparent font-serif text-5xl font-extrabold leading-tight tracking-normal text-[#07140c] outline-none placeholder:text-[#c2d0c0] md:text-6xl"
              defaultValue={blog?.title ?? ''}
              name="title"
              placeholder="Nhập tiêu đề bài viết..."
            />
          </label>

          <section className="mt-14 grid gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(320px,0.95fr)]">
            <div>
              <p className="mb-4 text-xs font-extrabold uppercase tracking-[0.16em] text-primary">Ảnh đại diện</p>
              <button
                className="grid aspect-[16/9] w-full place-items-center overflow-hidden rounded-lg border border-dashed border-primary/20 bg-[#e7f7e7] text-center transition hover:border-primary/50"
                onClick={() => fileInputRef.current?.click()}
                type="button"
              >
                {preview ? (
                  <img alt="Featured preview" className="h-full w-full object-cover" src={preview} />
                ) : (
                  <span className="flex flex-col items-center gap-3 px-6 text-text">
                    <Camera className="h-10 w-10 text-primary/45" />
                    <span className="text-lg font-semibold">Chọn ảnh đại diện bài viết</span>
                    <span className="text-sm text-primary/45">Khuyến nghị 1600 x 900px</span>
                  </span>
                )}
              </button>
              <input
                accept="image/*"
                className="hidden"
                name="thumbnailFile"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) setPreview(URL.createObjectURL(file));
                }}
                ref={fileInputRef}
                type="file"
              />
              <input
                className="input mt-3"
                defaultValue={blog?.thumbnailUrl ?? ''}
                onChange={(event) => setPreview(event.target.value)}
                placeholder="Hoặc dán thumbnail URL"
                type="url"
              />
            </div>

            <aside>
              <p className="mb-4 text-xs font-extrabold uppercase tracking-[0.16em] text-primary">Chủ đề</p>
              <div className="flex flex-wrap gap-3">
                {tagOptions.map((tag) => (
                  <button
                    className={`rounded-full px-5 py-3 text-sm font-semibold transition ${
                      tags.includes(tag) ? 'bg-primary text-white shadow-soft' : 'bg-primary-soft text-text hover:bg-primary-soft/80'
                    }`}
                    key={tag}
                    onClick={() => toggleTag(tag)}
                    type="button"
                  >
                    {tag}
                  </button>
                ))}
              </div>
              <div className="mt-7 rounded-lg bg-[#e3f6e4] p-7 font-serif text-lg italic leading-8 text-text">
                Bài viết sẽ được kiểm duyệt và đồng bộ vào cộng đồng Calories Tracker sau khi xuất bản.
              </div>
            </aside>
          </section>

          <div className="mt-12 flex items-center gap-2 border-b border-border pb-5">
            <button className="icon-btn" type="button"><Heading1 className="h-5 w-5" /></button>
            <button className="icon-btn" type="button"><Bold className="h-5 w-5" /></button>
            <button className="icon-btn" type="button"><List className="h-5 w-5" /></button>
            <button className="icon-btn" type="button"><Quote className="h-5 w-5" /></button>
            <span className="mx-2 h-6 w-px bg-border" />
            <button className="icon-btn" type="button"><LinkIcon className="h-5 w-5" /></button>
            <span className="ml-auto font-mono text-xs text-muted">{(textBlock?.text_content ?? textBlock?.textContent ?? '').split(/\s+/).filter(Boolean).length} words</span>
          </div>

          <textarea
            className="mt-7 min-h-72 w-full resize-y border-0 bg-transparent text-xl leading-9 text-text outline-none placeholder:text-muted"
            defaultValue={textBlock?.text_content ?? textBlock?.textContent ?? ''}
            name="text"
            placeholder="Bắt đầu viết nội dung bài viết..."
          />

          <label className="mt-6 block">
            <span className="mb-2 flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.16em] text-primary">
              <ImagePlus className="h-4 w-4" />
              Ảnh trong nội dung
            </span>
            <input className="input" defaultValue={imageBlock?.image_url ?? imageBlock?.imageUrl ?? ''} name="imageUrl" placeholder="Ảnh minh họa trong nội dung" />
          </label>

          {error ? (
            <div className="mt-5 rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
              {error}
            </div>
          ) : null}
        </div>

        <footer className="flex flex-col gap-3 border-t border-border bg-primary-faint/55 px-8 py-6 sm:flex-row sm:items-center sm:justify-between md:px-16">
          <div className="flex gap-3">
            <button className="btn-secondary border-0 bg-transparent" disabled={isSubmitting} name="status" onClick={() => setStatus('draft')} type="submit" value="draft">
              Lưu nháp
            </button>
            <button className="btn-secondary border-0 bg-transparent" disabled={isSubmitting} onClick={onClose} type="button">
              Hủy
            </button>
          </div>
          <button className="btn-primary rounded-full px-8 shadow-float" disabled={isSubmitting} name="status" onClick={() => setStatus('approved')} type="submit" value="approved">
            {isSubmitting ? 'Đang lưu' : 'Xuất bản cộng đồng'}
          </button>
        </footer>
      </form>
    </div>
  );
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

function labelBlogStatus(status?: BlogStatus) {
  switch (status) {
    case 'approved':
      return 'Đã duyệt';
    case 'pending':
      return 'Chờ duyệt';
    case 'rejected':
      return 'Từ chối';
    case 'draft':
      return 'Draft';
    default:
      return 'Unknown';
  }
}
