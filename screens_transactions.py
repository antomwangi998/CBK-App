# screens_transactions.py - Deposit, Withdrawal, Transfer screens
import sys as _sys, os as _os
_sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__)))

import datetime
import threading

from kivy.clock import Clock
from kivy.logger import Logger
from kivy.metrics import dp, sp
from kivy.uix.widget import Widget

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.button import MDFlatButton, MDRaisedButton
from kivymd.uix.card import MDCard
from kivymd.uix.dialog import MDDialog
from kivymd.uix.label import MDIcon, MDLabel
from kivymd.uix.scrollview import MDScrollView
from kivymd.uix.textfield import MDTextField
from kivymd.uix.toolbar import MDTopAppBar

from constants import get_color
from screens import BaseScreen


def _fmt(minor):
    try:
        return f"KSh {int(minor) / 100:,.2f}"
    except Exception:
        return "KSh 0.00"


def _to_minor(text):
    try:
        return int(float(str(text).replace(',', '')) * 100)
    except Exception:
        return 0

# Alias kept for backward compatibility
_amount_from_text = _to_minor


def _receipt_dialog(title, lines, on_dismiss=None):
    content = MDBoxLayout(
        orientation='vertical', spacing=dp(8), padding=dp(4),
        size_hint_y=None, height=dp(max(len(lines) * 36 + 16, 60))
    )
    for label, value in lines:
        row = MDBoxLayout(size_hint_y=None, height=dp(32))
        row.add_widget(MDLabel(
            text=str(label), theme_text_color='Secondary',
            size_hint_x=0.5, font_style='Caption',
            valign='middle'
        ))
        row.add_widget(MDLabel(
            text=str(value), bold=True, size_hint_x=0.5, halign='right',
            valign='middle'
        ))
        content.add_widget(row)

    dialog = MDDialog(
        title=f'✅  {title}',
        type='custom',
        content_cls=content,
        buttons=[
            MDRaisedButton(
                text='DONE',
                md_bg_color=get_color('primary'),
                on_release=lambda x: (dialog.dismiss(), on_dismiss() if on_dismiss else None)
            )
        ]
    )
    dialog.open()


# ─────────────────────────────────────────────────────────────────────────────
# DEPOSIT SCREEN
# ─────────────────────────────────────────────────────────────────────────────

class DepositScreen(BaseScreen):

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = 'deposit'
        self.member_id = None
        self._accounts = []
        self._selected_account_id = None
        self._channel = 'branch'
        self._build()

    def _build(self):
        root = MDBoxLayout(orientation='vertical')

        self.toolbar = MDTopAppBar(
            title='Deposit',
            md_bg_color=get_color('success'),
            specific_text_color=(1, 1, 1, 1),
            left_action_items=[['arrow-left', lambda x: self.app.go_back()]],
        )
        root.add_widget(self.toolbar)

        scroll = MDScrollView()
        body = MDBoxLayout(
            orientation='vertical', spacing=dp(14),
            padding=dp(14), size_hint_y=None
        )
        body.bind(minimum_height=body.setter('height'))

        # Member search row
        search_row = MDBoxLayout(size_hint_y=None, height=dp(56), spacing=dp(8))
        self.member_search = MDTextField(
            hint_text='Search member by name, ID or phone',
            mode='rectangle', line_color_focus=get_color('success')
        )
        search_btn = MDRaisedButton(
            text='Find',
            md_bg_color=get_color('success'),
            size_hint_x=None, width=dp(70),
            on_release=lambda x: self._search_member()
        )
        search_row.add_widget(self.member_search)
        search_row.add_widget(search_btn)
        body.add_widget(search_row)

        # Member banner
        self.member_card = MDCard(
            orientation='horizontal', size_hint_y=None, height=dp(64),
            radius=[dp(10)], padding=dp(12),
            md_bg_color=get_color('success', 0.08), elevation=0
        )
        self.member_icon = MDCard(
            size_hint=(None, None), size=(dp(40), dp(40)),
            radius=[dp(20)], md_bg_color=get_color('success', 0.2)
        )
        self.member_icon.add_widget(MDIcon(
            icon='account', theme_text_color='Custom',
            text_color=get_color('success'), halign='center',
            valign='middle'
        ))
        self.member_name_lbl = MDLabel(
            text='No member selected', font_style='Subtitle2',
            theme_text_color='Secondary',
            valign='middle'
        )
        self.member_card.add_widget(self.member_icon)
        self.member_card.add_widget(self.member_name_lbl)
        body.add_widget(self.member_card)

        # Account picker
        body.add_widget(self._sec('To Account', 'bank-outline'))
        self.account_box = MDBoxLayout(
            orientation='vertical', spacing=dp(6), size_hint_y=None
        )
        self.account_box.bind(minimum_height=self.account_box.setter('height'))
        body.add_widget(self.account_box)

        # Amount
        body.add_widget(self._sec('Amount (KSh)', 'cash-plus'))
        self.amount_field = MDTextField(
            hint_text='0.00', mode='rectangle',
            input_filter='float', font_size=sp(22),
            line_color_focus=get_color('success'),
            size_hint_y=None, height=dp(60)
        )
        body.add_widget(self.amount_field)

        # Quick amounts
        chips = MDBoxLayout(size_hint_y=None, height=dp(44), spacing=dp(8))
        for amt in [500, 1000, 2000, 5000, 10000]:
            c = MDCard(
                size_hint=(None, None), size=(dp(74), dp(36)),
                radius=[dp(18)],
                md_bg_color=get_color('success_container', 0.4),
                ripple_behavior=True,
                on_release=lambda x, a=amt: setattr(self.amount_field, 'text', str(a))
            )
            c.add_widget(MDLabel(
                text=f'{amt:,}', halign='center', font_style='Caption',
                theme_text_color='Custom', text_color=get_color('success'),
                valign='middle'
            ))
            chips.add_widget(c)
        body.add_widget(chips)

        self.desc_field = MDTextField(
            hint_text='Reference / Description (optional)',
            mode='rectangle', line_color_focus=get_color('success'),
            size_hint_y=None, height=dp(56)
        )
        body.add_widget(self.desc_field)

        # Channel selector
        body.add_widget(self._sec('Channel', 'swap-horizontal'))
        channel_row = MDBoxLayout(size_hint_y=None, height=dp(40), spacing=dp(8))
        self._channel_btns = {}
        for ch in ['branch', 'mobile', 'agent', 'online']:
            btn = MDCard(
                size_hint_x=1, size_hint_y=None, height=dp(38),
                radius=[dp(8)],
                md_bg_color=get_color('success') if ch == 'branch' else get_color('surface_variant', 0.4),
                ripple_behavior=True,
                on_release=lambda x, c=ch: self._pick_channel(c)
            )
            btn.add_widget(MDLabel(
                text=ch.title(), halign='center', font_style='Caption',
                theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if ch == 'branch' else get_color('outline'),
                valign='middle'
            ))
            self._channel_btns[ch] = btn
            channel_row.add_widget(btn)
        body.add_widget(channel_row)

        self.submit_btn = MDRaisedButton(
            text='DEPOSIT',
            size_hint_x=1, height=dp(54),
            md_bg_color=get_color('success'),
            font_size=sp(15),
            on_release=self._confirm
        )
        body.add_widget(MDBoxLayout(size_hint_y=None, height=dp(8)))
        body.add_widget(self.submit_btn)

        scroll.add_widget(body)
        root.add_widget(scroll)
        self.add_widget(root)

    def _sec(self, text, icon):
        row = MDBoxLayout(size_hint_y=None, height=dp(30), spacing=dp(8))
        row.add_widget(MDIcon(
            icon=icon, theme_text_color='Custom',
            text_color=get_color('success'), size_hint_x=None, width=dp(22),
            valign='middle'
        ))
        row.add_widget(MDLabel(
            text=text.upper(), font_style='Caption', bold=True,
            theme_text_color='Custom', text_color=get_color('success'),
            valign='middle'
        ))
        return row

    def _pick_channel(self, ch):
        self._channel = ch
        for c, btn in self._channel_btns.items():
            active = c == ch
            btn.md_bg_color = get_color('success') if active else get_color('surface_variant', 0.4)
            btn.children[0].text_color = (1, 1, 1, 1) if active else get_color('outline')

    def on_enter(self):
        if self.member_id:
            threading.Thread(target=self._load_member_by_id, args=(self.member_id,), daemon=True).start()

    def _search_member(self):
        q = self.member_search.text.strip()
        if not q:
            self.show_error('Enter a name, phone, or ID to search')
            return
        threading.Thread(target=self._run_member_search, args=(q,), daemon=True).start()

    def _run_member_search(self, q):
        try:
            m = self.app.db.fetch_one(
                "SELECT * FROM members WHERE is_active=1 AND "
                "(first_name LIKE ? OR last_name LIKE ? OR phone=? OR id_number=? OR member_no=?) LIMIT 1",
                (f'%{q}%', f'%{q}%', q, q, q)
            )
            if m:
                Clock.schedule_once(lambda dt: self._load_member(m), 0)
            else:
                Clock.schedule_once(lambda dt: self.show_error('Member not found'), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self.show_error(_e), 0)

    def _load_member_by_id(self, mid):
        try:
            m = self.app.db.fetch_one("SELECT * FROM members WHERE id=?", (mid,))
            accs = self.app.db.fetch_all(
                "SELECT * FROM accounts WHERE member_id=? AND status='active'", (mid,)
            )
            Clock.schedule_once(lambda dt: (self._update_member_banner(m), self._render_accounts(accs)), 0)
        except Exception as e:
            Logger.error(f'Deposit load: {e}')

    def _load_member(self, m):
        self.member_id = m['id']
        self._update_member_banner(m)
        threading.Thread(target=self._load_accounts, daemon=True).start()

    def _update_member_banner(self, m):
        if m:
            self.member_name_lbl.text = f"{m.get('first_name','')} {m.get('last_name','')}  •  {m.get('member_no','')}"
            self.member_name_lbl.theme_text_color = 'Custom'
            self.member_name_lbl.text_color = get_color('success')

    def _load_accounts(self):
        try:
            accs = self.app.db.fetch_all(
                "SELECT * FROM accounts WHERE member_id=? AND status='active' AND account_type!='loan'",
                (self.member_id,)
            )
            Clock.schedule_once(lambda dt: self._render_accounts(accs), 0)
        except Exception as e:
            Logger.error(f'Deposit accounts: {e}')

    def _render_accounts(self, accs):
        self._accounts = accs
        self.account_box.clear_widgets()
        if not accs:
            self.account_box.add_widget(MDLabel(
                text='No accounts found for this member',
                theme_text_color='Secondary', size_hint_y=None, height=dp(36),
                valign='middle'
            ))
            return
        if not self._selected_account_id and accs:
            self._selected_account_id = accs[0]['id']
        for acc in accs:
            active = acc['id'] == self._selected_account_id
            card = MDCard(
                orientation='horizontal', size_hint_y=None, height=dp(60),
                radius=[dp(10)], padding=dp(12),
                md_bg_color=get_color('success') if active else get_color('surface_variant', 0.2),
                ripple_behavior=True, elevation=2 if active else 0,
                on_release=lambda x, a=acc: self._select_account(a)
            )
            card.add_widget(MDIcon(
                icon='bank', theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('success'),
                size_hint_x=None, width=dp(28),
                valign='middle'
            ))
            info = MDBoxLayout(orientation='vertical')
            info.add_widget(MDLabel(
                text=acc.get('account_no', ''), font_style='Subtitle2', bold=True,
                theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('on_surface'),
                valign='middle'
            ))
            info.add_widget(MDLabel(
                text=f"{acc.get('account_type','').replace('_',' ').title()}  •  Bal: {_fmt(acc.get('balance_minor',0))}",
                font_style='Caption', theme_text_color='Custom',
                text_color=(1, 1, 1, 0.8) if active else get_color('outline'),
                valign='middle'
            ))
            card.add_widget(info)
            if active:
                card.add_widget(MDIcon(
                    icon='check-circle', theme_text_color='Custom',
                    text_color=(1, 1, 1, 1), size_hint_x=None, width=dp(28),
                    valign='middle'
                ))
            self.account_box.add_widget(card)

    def _select_account(self, acc):
        self._selected_account_id = acc['id']
        self._render_accounts(self._accounts)

    def _confirm(self, *args):
        if not self.member_id:
            self.show_error('Search and select a member first')
            return
        if not self._selected_account_id:
            self.show_error('Select an account')
            return
        amount = _to_minor(self.amount_field.text)
        if amount <= 0:
            self.show_error('Enter a valid amount')
            return
        acc = next((a for a in self._accounts if a['id'] == self._selected_account_id), {})
        dialog = MDDialog(
            title='Confirm Deposit',
            text=f"Deposit [b]{_fmt(amount)}[/b] into {acc.get('account_no', '')}?",
            buttons=[
                MDFlatButton(text='CANCEL', on_release=lambda x: dialog.dismiss()),
                MDRaisedButton(
                    text='CONFIRM', md_bg_color=get_color('success'),
                    on_release=lambda x: (dialog.dismiss(), self._execute(amount))
                )
            ]
        )
        dialog.open()

    def _execute(self, amount):
        self.submit_btn.disabled = True
        self.submit_btn.text = 'Processing…'
        desc = self.desc_field.text.strip() or 'Cash deposit'
        threading.Thread(target=self._run, args=(amount, desc), daemon=True).start()

    def _run(self, amount, desc):
        try:
            self.app.account_service.post_transaction(
                self._selected_account_id, 'deposit', amount, desc, channel=self._channel
            )
            acc = self.app.db.fetch_one("SELECT * FROM accounts WHERE id=?", (self._selected_account_id,))
            Clock.schedule_once(lambda dt: self._on_success(amount, acc), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self._on_error(_e), 0)

    def _on_success(self, amount, acc):
        self.submit_btn.disabled = False
        self.submit_btn.text = 'DEPOSIT'
        self.amount_field.text = ''
        self.desc_field.text = ''
        _receipt_dialog('Deposit Successful', [
            ('Account', acc.get('account_no', '')),
            ('Amount', _fmt(amount)),
            ('New Balance', _fmt(acc.get('balance_minor', 0))),
            ('Date', datetime.datetime.now().strftime('%d %b %Y %H:%M')),
        ], on_dismiss=lambda: threading.Thread(target=self._load_accounts, daemon=True).start())

    def _on_error(self, msg):
        self.submit_btn.disabled = False
        self.submit_btn.text = 'DEPOSIT'
        self.show_error(msg)


# ─────────────────────────────────────────────────────────────────────────────
# WITHDRAWAL SCREEN
# ─────────────────────────────────────────────────────────────────────────────

class WithdrawalScreen(BaseScreen):

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = 'withdrawal'
        self.member_id = None
        self._accounts = []
        self._selected_account_id = None
        self._selected_acc = {}
        self._build()

    def _build(self):
        root = MDBoxLayout(orientation='vertical')
        self.toolbar = MDTopAppBar(
            title='Withdraw',
            md_bg_color=get_color('error'),
            specific_text_color=(1, 1, 1, 1),
            left_action_items=[['arrow-left', lambda x: self.app.go_back()]],
        )
        root.add_widget(self.toolbar)

        scroll = MDScrollView()
        body = MDBoxLayout(
            orientation='vertical', spacing=dp(14),
            padding=dp(14), size_hint_y=None
        )
        body.bind(minimum_height=body.setter('height'))

        # Member search
        search_row = MDBoxLayout(size_hint_y=None, height=dp(56), spacing=dp(8))
        self.member_search = MDTextField(
            hint_text='Search member…', mode='rectangle',
            line_color_focus=get_color('error')
        )
        search_row.add_widget(self.member_search)
        search_row.add_widget(MDRaisedButton(
            text='Find', md_bg_color=get_color('error'),
            size_hint_x=None, width=dp(70),
            on_release=lambda x: self._search_member()
        ))
        body.add_widget(search_row)

        # Balance banner
        self.balance_card = MDCard(
            orientation='vertical', size_hint_y=None, height=dp(70),
            radius=[dp(10)], padding=dp(14),
            md_bg_color=get_color('error'), elevation=3
        )
        self.balance_card.add_widget(MDLabel(
            text='Available Balance', font_style='Caption',
            theme_text_color='Custom', text_color=(1, 1, 1, 0.8),
            size_hint_y=None, height=dp(20),
            valign='middle'
        ))
        self.balance_lbl = MDLabel(
            text='KSh 0.00', font_style='H5', bold=True,
            theme_text_color='Custom', text_color=(1, 1, 1, 1),
            valign='middle'
        )
        self.balance_card.add_widget(self.balance_lbl)
        body.add_widget(self.balance_card)

        # Account picker
        body.add_widget(self._sec('From Account', 'bank-minus'))
        self.account_box = MDBoxLayout(
            orientation='vertical', spacing=dp(6), size_hint_y=None
        )
        self.account_box.bind(minimum_height=self.account_box.setter('height'))
        body.add_widget(self.account_box)

        # Amount
        body.add_widget(self._sec('Amount (KSh)', 'cash-minus'))
        self.amount_field = MDTextField(
            hint_text='0.00', mode='rectangle',
            input_filter='float', font_size=sp(22),
            line_color_focus=get_color('error'),
            size_hint_y=None, height=dp(60)
        )
        body.add_widget(self.amount_field)

        chips = MDBoxLayout(size_hint_y=None, height=dp(44), spacing=dp(8))
        for amt in [500, 1000, 2000, 5000, 10000]:
            c = MDCard(
                size_hint=(None, None), size=(dp(74), dp(36)),
                radius=[dp(18)],
                md_bg_color=get_color('error_container', 0.4),
                ripple_behavior=True,
                on_release=lambda x, a=amt: setattr(self.amount_field, 'text', str(a))
            )
            c.add_widget(MDLabel(
                text=f'{amt:,}', halign='center', font_style='Caption',
                theme_text_color='Custom', text_color=get_color('error'),
                valign='middle'
            ))
            chips.add_widget(c)
        body.add_widget(chips)

        self.desc_field = MDTextField(
            hint_text='Purpose / Reference',
            mode='rectangle', line_color_focus=get_color('error'),
            size_hint_y=None, height=dp(56)
        )
        body.add_widget(self.desc_field)

        self.submit_btn = MDRaisedButton(
            text='WITHDRAW', size_hint_x=1, height=dp(54),
            md_bg_color=get_color('error'), font_size=sp(15),
            on_release=self._confirm
        )
        body.add_widget(MDBoxLayout(size_hint_y=None, height=dp(8)))
        body.add_widget(self.submit_btn)

        scroll.add_widget(body)
        root.add_widget(scroll)
        self.add_widget(root)

    def _sec(self, text, icon):
        row = MDBoxLayout(size_hint_y=None, height=dp(30), spacing=dp(8))
        row.add_widget(MDIcon(
            icon=icon, theme_text_color='Custom',
            text_color=get_color('error'), size_hint_x=None, width=dp(22),
            valign='middle'
        ))
        row.add_widget(MDLabel(
            text=text.upper(), font_style='Caption', bold=True,
            theme_text_color='Custom', text_color=get_color('error'),
            valign='middle'
        ))
        return row

    def on_enter(self):
        if self.member_id:
            threading.Thread(target=self._load_by_id, args=(self.member_id,), daemon=True).start()

    def _search_member(self):
        q = self.member_search.text.strip()
        if not q:
            return
        threading.Thread(target=self._run_search, args=(q,), daemon=True).start()

    def _run_search(self, q):
        try:
            m = self.app.db.fetch_one(
                "SELECT * FROM members WHERE is_active=1 AND "
                "(first_name LIKE ? OR last_name LIKE ? OR phone=? OR id_number=?) LIMIT 1",
                (f'%{q}%', f'%{q}%', q, q)
            )
            if m:
                self.member_id = m['id']
                Clock.schedule_once(lambda dt: threading.Thread(target=self._load_by_id, args=(m['id'],), daemon=True).start(), 0)
            else:
                Clock.schedule_once(lambda dt: self.show_error('Member not found'), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self.show_error(_e), 0)

    def _load_by_id(self, mid):
        try:
            accs = self.app.db.fetch_all(
                "SELECT * FROM accounts WHERE member_id=? AND status='active' AND account_type!='loan'", (mid,)
            )
            Clock.schedule_once(lambda dt: self._render_accounts(accs), 0)
        except Exception as e:
            Logger.error(f'Withdrawal: {e}')

    def _render_accounts(self, accs):
        self._accounts = accs
        self.account_box.clear_widgets()
        if not accs:
            self.account_box.add_widget(MDLabel(
                text='No accounts found', theme_text_color='Secondary',
                size_hint_y=None, height=dp(36),
                valign='middle'
            ))
            return
        if not self._selected_account_id:
            self._select_account(accs[0])
        for acc in accs:
            active = acc['id'] == self._selected_account_id
            card = MDCard(
                orientation='horizontal', size_hint_y=None, height=dp(60),
                radius=[dp(10)], padding=dp(12),
                md_bg_color=get_color('error') if active else get_color('surface_variant', 0.2),
                ripple_behavior=True, elevation=2 if active else 0,
                on_release=lambda x, a=acc: self._select_account(a)
            )
            card.add_widget(MDIcon(
                icon='bank', theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('error'),
                size_hint_x=None, width=dp(28),
                valign='middle'
            ))
            info = MDBoxLayout(orientation='vertical')
            info.add_widget(MDLabel(
                text=acc.get('account_no', ''), font_style='Subtitle2',
                theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('on_surface'),
                valign='middle'
            ))
            info.add_widget(MDLabel(
                text=f"Available: {_fmt(acc.get('available_balance_minor', acc.get('balance_minor', 0)))}",
                font_style='Caption', theme_text_color='Custom',
                text_color=(1, 1, 1, 0.8) if active else get_color('outline'),
                valign='middle'
            ))
            card.add_widget(info)
            self.account_box.add_widget(card)

    def _select_account(self, acc):
        self._selected_account_id = acc['id']
        self._selected_acc = acc
        avail = acc.get('available_balance_minor', acc.get('balance_minor', 0))
        self.balance_lbl.text = _fmt(avail)
        self._render_accounts(self._accounts)

    def _confirm(self, *args):
        if not self.member_id or not self._selected_account_id:
            self.show_error('Select a member and account first')
            return
        amount = _to_minor(self.amount_field.text)
        if amount <= 0:
            self.show_error('Enter a valid amount')
            return
        avail = self._selected_acc.get('available_balance_minor',
                                        self._selected_acc.get('balance_minor', 0))
        if amount > avail:
            self.show_error(f'Insufficient balance. Available: {_fmt(avail)}')
            return

        dialog = MDDialog(
            title='Confirm Withdrawal',
            text=f"Withdraw [b]{_fmt(amount)}[/b] from {self._selected_acc.get('account_no', '')}?",
            buttons=[
                MDFlatButton(text='CANCEL', on_release=lambda x: dialog.dismiss()),
                MDRaisedButton(
                    text='CONFIRM', md_bg_color=get_color('error'),
                    on_release=lambda x: (dialog.dismiss(), self._execute(amount))
                )
            ]
        )
        dialog.open()

    def _execute(self, amount):
        self.submit_btn.disabled = True
        self.submit_btn.text = 'Processing…'
        desc = self.desc_field.text.strip() or 'Cash withdrawal'
        threading.Thread(target=self._run, args=(amount, desc), daemon=True).start()

    def _run(self, amount, desc):
        try:
            self.app.account_service.post_transaction(
                self._selected_account_id, 'withdrawal', amount, desc
            )
            acc = self.app.db.fetch_one("SELECT * FROM accounts WHERE id=?", (self._selected_account_id,))
            Clock.schedule_once(lambda dt: self._on_success(amount, acc), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self._on_error(_e), 0)

    def _on_success(self, amount, acc):
        self.submit_btn.disabled = False
        self.submit_btn.text = 'WITHDRAW'
        self.amount_field.text = ''
        _receipt_dialog('Withdrawal Successful', [
            ('Account', acc.get('account_no', '')),
            ('Amount', _fmt(amount)),
            ('Balance', _fmt(acc.get('balance_minor', 0))),
            ('Date', datetime.datetime.now().strftime('%d %b %Y %H:%M')),
        ], on_dismiss=lambda: threading.Thread(target=self._load_by_id, args=(self.member_id,), daemon=True).start())

    def _on_error(self, msg):
        self.submit_btn.disabled = False
        self.submit_btn.text = 'WITHDRAW'
        self.show_error(msg)


# ─────────────────────────────────────────────────────────────────────────────
# TRANSFER SCREEN
# ─────────────────────────────────────────────────────────────────────────────

class TransferScreen(BaseScreen):

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = 'transfer'
        self.member_id = None
        self._from_accounts = []
        self._from_id = None
        self._to_account_id = None
        self._to_label = ''
        self._build()

    def _build(self):
        root = MDBoxLayout(orientation='vertical')
        self.toolbar = MDTopAppBar(
            title='Transfer',
            md_bg_color=get_color('secondary'),
            specific_text_color=(1, 1, 1, 1),
            left_action_items=[['arrow-left', lambda x: self.app.go_back()]],
        )
        root.add_widget(self.toolbar)

        scroll = MDScrollView()
        body = MDBoxLayout(
            orientation='vertical', spacing=dp(14),
            padding=dp(14), size_hint_y=None
        )
        body.bind(minimum_height=body.setter('height'))

        # From member search
        search_row = MDBoxLayout(size_hint_y=None, height=dp(56), spacing=dp(8))
        self.from_search = MDTextField(
            hint_text='From member (name/phone/ID)…',
            mode='rectangle', line_color_focus=get_color('secondary')
        )
        search_row.add_widget(self.from_search)
        search_row.add_widget(MDRaisedButton(
            text='Find', md_bg_color=get_color('secondary'),
            size_hint_x=None, width=dp(70),
            on_release=lambda x: self._search_from()
        ))
        body.add_widget(search_row)

        body.add_widget(self._sec('From Account', 'arrow-top-right'))
        self.from_box = MDBoxLayout(
            orientation='vertical', spacing=dp(6), size_hint_y=None
        )
        self.from_box.bind(minimum_height=self.from_box.setter('height'))
        body.add_widget(self.from_box)

        body.add_widget(self._sec('To Account', 'arrow-bottom-left'))
        to_row = MDBoxLayout(size_hint_y=None, height=dp(56), spacing=dp(8))
        self.to_search = MDTextField(
            hint_text='Destination account number…',
            mode='rectangle', line_color_focus=get_color('secondary')
        )
        to_row.add_widget(self.to_search)
        to_row.add_widget(MDRaisedButton(
            text='Find', md_bg_color=get_color('secondary'),
            size_hint_x=None, width=dp(70),
            on_release=lambda x: self._search_to()
        ))
        body.add_widget(to_row)

        self.to_result = MDCard(
            orientation='horizontal', size_hint_y=None, height=dp(0),
            radius=[dp(10)], padding=dp(12),
            md_bg_color=get_color('secondary_container', 0.3), elevation=0,
            opacity=0
        )
        self.to_lbl = MDLabel(text='', font_style='Subtitle2', valign='middle')
        from kivy.uix.relativelayout import RelativeLayout
        from kivy.graphics import Color as _C, RoundedRectangle as _RR
        ic_rl = RelativeLayout(size_hint=(None, None), size=(dp(28), dp(28)))
        with ic_rl.canvas.before:
            _C(*get_color('success_container', 0.6))
            _RR(pos=(0, 0), size=(dp(28), dp(28)), radius=[dp(14)])
        ic_rl.add_widget(MDIcon(
            icon='check-circle-outline', theme_text_color='Custom',
            text_color=get_color('success'), halign='center', valign='middle',
            font_size=sp(18), size_hint=(None, None), size=(dp(20), dp(20)),
            pos_hint={'center_x': 0.5, 'center_y': 0.5}
        ))
        self.to_result.add_widget(ic_rl)
        self.to_result.add_widget(self.to_lbl)
        body.add_widget(self.to_result)

        body.add_widget(self._sec('Amount (KSh)', 'bank-transfer'))
        self.amount_field = MDTextField(
            hint_text='0.00', mode='rectangle',
            input_filter='float', font_size=sp(22),
            line_color_focus=get_color('secondary'),
            size_hint_y=None, height=dp(60)
        )
        body.add_widget(self.amount_field)

        self.desc_field = MDTextField(
            hint_text='Narration / Reference',
            mode='rectangle', line_color_focus=get_color('secondary'),
            size_hint_y=None, height=dp(56)
        )
        body.add_widget(self.desc_field)

        self.submit_btn = MDRaisedButton(
            text='TRANSFER', size_hint_x=1, height=dp(54),
            md_bg_color=get_color('secondary'), font_size=sp(15),
            on_release=self._confirm
        )
        body.add_widget(MDBoxLayout(size_hint_y=None, height=dp(8)))
        body.add_widget(self.submit_btn)

        scroll.add_widget(body)
        root.add_widget(scroll)
        self.add_widget(root)

    def _sec(self, text, icon):
        row = MDBoxLayout(size_hint_y=None, height=dp(30), spacing=dp(8))
        row.add_widget(MDIcon(
            icon=icon, theme_text_color='Custom',
            text_color=get_color('secondary'), size_hint_x=None, width=dp(22),
            valign='middle'
        ))
        row.add_widget(MDLabel(
            text=text.upper(), font_style='Caption', bold=True,
            theme_text_color='Custom', text_color=get_color('secondary'),
            valign='middle'
        ))
        return row

    def on_enter(self):
        # Reset destination result card (hide it cleanly every visit)
        self.to_result.height = dp(0)
        self.to_result.opacity = 0
        self.to_lbl.text = ''
        self._to_account_id = None
        self.to_search.text = ''
        self.amount_field.text = ''
        self.desc_field.text = ''
        self.submit_btn.disabled = False
        self.submit_btn.text = 'TRANSFER'
        if self.member_id:
            threading.Thread(target=self._load_from, args=(self.member_id,), daemon=True).start()

    def _search_from(self):
        q = self.from_search.text.strip()
        if not q:
            return
        threading.Thread(target=self._run_from_search, args=(q,), daemon=True).start()

    def _run_from_search(self, q):
        try:
            m = self.app.db.fetch_one(
                "SELECT * FROM members WHERE is_active=1 AND "
                "(first_name LIKE ? OR last_name LIKE ? OR phone=?) LIMIT 1",
                (f'%{q}%', f'%{q}%', q)
            )
            if m:
                self.member_id = m['id']
                Clock.schedule_once(lambda dt: threading.Thread(
                    target=self._load_from, args=(m['id'],), daemon=True
                ).start(), 0)
            else:
                Clock.schedule_once(lambda dt: self.show_error('Member not found'), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self.show_error(_e), 0)

    def _load_from(self, mid):
        try:
            accs = self.app.db.fetch_all(
                "SELECT * FROM accounts WHERE member_id=? AND status='active' AND account_type!='loan'", (mid,)
            )
            Clock.schedule_once(lambda dt: self._render_from(accs), 0)
        except Exception as e:
            Logger.error(f'Transfer from: {e}')

    def _render_from(self, accs):
        self._from_accounts = accs
        self.from_box.clear_widgets()
        if not accs:
            self.from_box.add_widget(MDLabel(text='No accounts', theme_text_color='Secondary', size_hint_y=None, height=dp(36),
                valign='middle'
            ))
            return
        if not self._from_id:
            self._from_id = accs[0]['id']
        for acc in accs:
            active = acc['id'] == self._from_id
            card = MDCard(
                orientation='horizontal', size_hint_y=None, height=dp(56),
                radius=[dp(10)], padding=dp(12),
                md_bg_color=get_color('secondary') if active else get_color('surface_variant', 0.2),
                ripple_behavior=True, elevation=1,
                on_release=lambda x, a=acc: self._select_from(a)
            )
            card.add_widget(MDIcon(
                icon='bank', theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('secondary'),
                size_hint_x=None, width=dp(28),
                valign='middle'
            ))
            info = MDBoxLayout(orientation='vertical')
            info.add_widget(MDLabel(
                text=acc.get('account_no', ''), font_style='Subtitle2',
                theme_text_color='Custom', text_color=(1, 1, 1, 1) if active else get_color('on_surface'),
                valign='middle'
            ))
            info.add_widget(MDLabel(
                text=f"Bal: {_fmt(acc.get('balance_minor', 0))}",
                font_style='Caption', theme_text_color='Custom',
                text_color=(1, 1, 1, 0.8) if active else get_color('outline'),
                valign='middle'
            ))
            card.add_widget(info)
            self.from_box.add_widget(card)

    def _select_from(self, acc):
        self._from_id = acc['id']
        self._render_from(self._from_accounts)

    def _search_to(self):
        q = self.to_search.text.strip()
        if not q:
            return
        threading.Thread(target=self._run_to_search, args=(q,), daemon=True).start()

    def _run_to_search(self, q):
        try:
            acc = self.app.db.fetch_one(
                "SELECT a.*, m.first_name, m.last_name FROM accounts a "
                "JOIN members m ON a.member_id=m.id "
                "WHERE a.account_no=? AND a.status='active'", (q,)
            )
            Clock.schedule_once(lambda dt: self._show_to(acc), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self.show_error(_e), 0)

    def _show_to(self, acc):
        if not acc:
            self.show_error('Account not found')
            return
        self._to_account_id = acc['id']
        self._to_label = (
            f"{acc.get('first_name', '')} {acc.get('last_name', '')}  "
            f"•  {acc.get('account_no', '')}"
        )
        self.to_lbl.text = self._to_label
        from kivy.animation import Animation
        Animation(height=dp(52), opacity=1, duration=0.2).start(self.to_result)

    def _confirm(self, *args):
        if not self._from_id:
            self.show_error('Select source account')
            return
        if not self._to_account_id:
            self.show_error('Search and select destination account')
            return
        if self._from_id == self._to_account_id:
            self.show_error('Source and destination cannot be the same')
            return
        amount = _to_minor(self.amount_field.text)
        if amount <= 0:
            self.show_error('Enter valid amount')
            return

        dialog = MDDialog(
            title='Confirm Transfer',
            text=f"Transfer [b]{_fmt(amount)}[/b] to [b]{self._to_label}[/b]?",
            buttons=[
                MDFlatButton(text='CANCEL', on_release=lambda x: dialog.dismiss()),
                MDRaisedButton(
                    text='TRANSFER', md_bg_color=get_color('secondary'),
                    on_release=lambda x: (dialog.dismiss(), self._execute(amount))
                )
            ]
        )
        dialog.open()

    def _execute(self, amount):
        self.submit_btn.disabled = True
        self.submit_btn.text = 'Processing…'
        desc = self.desc_field.text.strip() or 'Member transfer'
        threading.Thread(target=self._run, args=(amount, desc), daemon=True).start()

    def _run(self, amount, desc):
        try:
            self.app.account_service.transfer(self._from_id, self._to_account_id, amount, desc)
            Clock.schedule_once(lambda dt: self._on_success(amount), 0)
        except Exception as e:
            Clock.schedule_once(lambda dt, _e=str(e): self._on_error(_e), 0)

    def _on_success(self, amount):
        self.submit_btn.disabled = False
        self.submit_btn.text = 'TRANSFER'
        self.amount_field.text = ''
        _receipt_dialog('Transfer Successful', [
            ('Amount', _fmt(amount)),
            ('To', self._to_label),
            ('Date', datetime.datetime.now().strftime('%d %b %Y %H:%M')),
        ])

    def _on_error(self, msg):
        self.submit_btn.disabled = False
        self.submit_btn.text = 'TRANSFER'
        self.show_error(msg)
