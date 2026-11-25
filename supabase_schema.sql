-- BD Nurses Directory — Supabase PostgreSQL Schema
-- Run this script in your Supabase Dashboard -> SQL Editor

create table if not exists public.nurses (
  phone text primary key,
  role text not null default 'user',
  status text not null default 'pending',
  pw text not null,
  must_change boolean not null default true,
  category text not null default '',
  name text not null,
  father text not null default '',
  mother text not null default '',
  dob text not null default '',
  prl text not null default '',
  join_date text not null default '',
  bnmc text not null default '',
  designation text not null default '',
  workplace text not null default '',
  division text not null default '',
  district text not null default '',
  upazila text not null default '',
  address text not null default '',
  education text not null default '',
  edu_cat text not null default '',
  edu_sub text not null default '',
  edu_info text not null default '',
  email text not null default '',
  photo text,
  pending_edit jsonb,
  reset_req jsonb,
  created_at timestamptz not null default now()
);

-- Grant API access to anon and authenticated roles
grant usage on schema public to anon, authenticated;
grant all on table public.nurses to anon, authenticated;

-- Enable Row Level Security
alter table public.nurses enable row level security;

-- Public directory access policy (required for client-side phone/SHA-256 auth flow)
-- Note: For higher security in production, consider migrating to Supabase Auth + Edge Functions.
drop policy if exists "Allow anon directory access" on public.nurses;
create policy "Allow anon directory access"
  on public.nurses
  for all
  to anon, authenticated
  using (true)
  with check (true);

-- Seed default Administrator account (ID: admin, Default Password: admin123)
insert into public.nurses (phone, role, name, pw, must_change, status)
values (
  'admin',
  'admin',
  'Administrator',
  '555b047b8e2c41bad0c4a365150d6ce30d51875fee13d718b8c6898860eea654',
  true,
  'approved'
)
on conflict (phone) do nothing;
