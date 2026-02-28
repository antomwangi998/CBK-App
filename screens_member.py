# screens_member.py — Member-facing production screens
# Statement Screen + Loan Calculator Screen
import sys as _sys, os as _os
_sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__)))

import datetime
import threading

from kivy.clock import Clock
from kivy.logger import Logger
from kivy.metrics import dp, sp
from kivy.uix.relativelayout import RelativeLayout
from kivy.graphics import Color, RoundedRectangle

from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.button import MDFlatButton, MDRaisedButton
from kivymd.uix.card import MDCard
from kivymd.uix.dialog import MDDialog
from kivymd.uix.gridlayout import MDGridLayout
from kivymd.uix.label import MDIcon, MDLabel
from kivymd.uix.scrollview import MDScrollView
from kivymd.uix.textfield import MDTextField
from kivymd.uix.toolbar import MDTopAppBar

from constants import get_color
from screens import BaseScreen


def _fmt(minor):
    return f"KSh {(minor or 0) / 100:,.2f}"


# ============================================================================
# ACCOUNT STATEMENT SCREEN
# ============================================================================

class StatementScreen(BaseScreen):
    """Full account statement — shows transactions, running balance, filters."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = 'statement'
        self._all_txns = []
        self._account_id = None
        self._build()

    def _build(self):
        from kivy.uix.floatlayout import FloatLayout
        float_root = FloatLayout()
        root = MDBoxLayout(orientation='vertical', size_hint=(1, 1))

        toolbar = MDTopAppBar(
            title='Account Statement',
            elevation=2,
            md_bg_color=get_color('primary'),
            specific_text_color=(1, 1, 1, 1),
            left_action_items=[['arrow-left', lambda x: self.app.navigate_back()]],
            right_action_items=[['download', lambda x: self._export()]],
        )
        root.add_widget(toolbar)

        scroll = MDScrollView(size_hint=(1, 1))
        content = MDBoxLayout(
            orientation='vertical', spacing=dp(10),
            padding=[dp(14), dp(14), dp(14), dp(20)],
            size_hint_y=None
        )
        content.bind(minimum_height=content.setter('height'))

        # ── Account summary card ───────────────────────────────────
        self._summary_card = MDCard(
            orientation='horizontal',
            padding=[dp(16), dp(14)], spacing=dp(12),
            radius=[dp(14)], md_bg_color=get_color('primary'),
            size_hint_y=None, height=dp(80), elevation=3
        )
        self._acc_no_lbl = MDLabel(
            text='Account: —', font_style='Subtitle1', bold=True,
            theme_text_color='Custom', text_color=(1, 1, 1, 1),
            size_hint_y=None, height=dp(26), valign='middle'
        )
        self._bal_lbl = MDLabel(
            text='Balance: KSh 0.00', font_style='Caption',
            theme_text_color='Custom', text_color=(1, 1, 1, 0.8),
            size_hint_y=None, height=dp(20), valign='middle'
        )
        info_col = MDBoxLayout(orientation='vertical', spacing=dp(2))
        info_col.add_widget(self._acc_no_lbl)
        info_col.add_widget(self._bal_lbl)
        self._summary_card.add_widget(info_col)
        content.add_widget(self._summary_card)

        # ── Date-range filter row ──────────────────────────────────
        filter_row = MDBoxLayout(
            size_hint_y=None, height=dp(44), spacing=dp(8)
        )
        self._period_btns = {}
        for key, label in [('7d','7 Days'),('30d','30 Days'),('90d','3 Months'),('all','All Time')]:
            active = key == '30d'
            btn = MDCard(
                size_hint=(None, None), size=(dp(74), dp(36)),
                radius=[dp(18)],
                md_bg_color=get_color('primary') if active else get_color('surface_variant', 0.4),
                ripple_behavior=True,
                on_release=lambda x, k=key: self._set_period(k)
            )
            lbl = MDLabel(
                text=label, halign='center', valign='middle',
                font_style='Caption', bold=active,
                theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('on_surface')
            )
            btn.add_widget(lbl)
            self._period_btns[key] = (btn, lbl)
            filter_row.add_widget(btn)
        self._active_period = '30d'
        content.add_widget(filter_row)

        # ── Totals strip ───────────────────────────────────────────
        totals_row = MDGridLayout(
            cols=3, spacing=dp(8), size_hint_y=None, height=dp(64)
        )
        self._total_in_card  = self._mini_card('KSh 0', 'Total In',  'success')
        self._total_out_card = self._mini_card('KSh 0', 'Total Out', 'error')
        self._txn_count_card = self._mini_card('0',     'Transactions', 'secondary')
        totals_row.add_widget(self._total_in_card)
        totals_row.add_widget(self._total_out_card)
        totals_row.add_widget(self._txn_count_card)
        content.add_widget(totals_row)

        # ── Transaction list ───────────────────────────────────────
        content.add_widget(MDLabel(
            text='TRANSACTIONS', font_style='Caption',
            theme_text_color='Secondary', bold=True,
            size_hint_y=None, height=dp(22), valign='middle'
        ))
        self._txn_list = MDBoxLayout(
            orientation='vertical', spacing=dp(6), size_hint_y=None
        )
        self._txn_list.bind(minimum_height=self._txn_list.setter('height'))
        content.add_widget(self._txn_list)

        scroll.add_widget(content)
        root.add_widget(scroll)
        float_root.add_widget(root)
        self.add_widget(float_root)

    def _mini_card(self, value, label, color):
        card = MDCard(
            orientation='vertical', padding=[dp(8), dp(6)],
            radius=[dp(10)],
            md_bg_color=get_color(f'{color}_container', 0.25),
            elevation=0
        )
        v = MDLabel(
            text=value, font_style='Subtitle2', bold=True,
            theme_text_color='Custom', text_color=get_color(color),
            halign='center', valign='middle',
            size_hint_y=None, height=dp(26)
        )
        l = MDLabel(
            text=label, font_style='Caption',
            theme_text_color='Secondary',
            halign='center', valign='middle',
            size_hint_y=None, height=dp(18)
        )
        card.add_widget(v)
        card.add_widget(l)
        card._v = v
        return card

    def on_enter(self):
        threading.Thread(target=self._load, daemon=True).start()

    def _load(self):
        try:
            uid = self.app.current_user_id
            role = self.app.current_user_role or 'member'
            if role == 'member':
                user = self.app.db.fetch_one("SELECT member_id FROM users WHERE id=?", (uid,))
                mid = (user or {}).get('member_id')
                acc = self.app.db.fetch_one(
                    "SELECT * FROM accounts WHERE member_id=? AND account_type='savings'", (mid,)
                ) if mid else None
            else:
                # staff — use member_id passed via navigate kwargs if available
                mid = getattr(self, '_target_member_id', None)
                acc = self.app.db.fetch_one(
                    "SELECT * FROM accounts WHERE member_id=? AND account_type='savings'", (mid,)
                ) if mid else None

            if not acc:
                Clock.schedule_once(lambda dt: self._show_no_account(), 0)
                return

            self._account_id = acc.get('id')
            txns = self.app.db.fetch_all(
                "SELECT * FROM transactions WHERE account_id=? ORDER BY posted_date DESC LIMIT 200",
                (self._account_id,)
            )
            Clock.schedule_once(lambda dt: self._render(acc, txns), 0)
        except Exception as e:
            Logger.error(f'Statement load: {e}')
            import traceback; traceback.print_exc()

    def _show_no_account(self):
        self._txn_list.clear_widgets()
        self._txn_list.add_widget(MDLabel(
            text='No savings account found.',
            halign='center', theme_text_color='Secondary',
            size_hint_y=None, height=dp(60), valign='middle'
        ))

    def _render(self, acc, txns):
        self._all_txns = txns
        self._acc_no_lbl.text = f"Account: {acc.get('account_no', '—')}"
        bal = (acc.get('balance_minor') or 0) / 100
        self._bal_lbl.text = f"Balance: KSh {bal:,.2f}"
        self._apply_period(self._active_period)

    def _set_period(self, key):
        self._active_period = key
        for k, (btn, lbl) in self._period_btns.items():
            active = k == key
            btn.md_bg_color = get_color('primary') if active else get_color('surface_variant', 0.4)
            lbl.text_color = (1, 1, 1, 1) if active else get_color('on_surface')
            lbl.bold = active
        self._apply_period(key)

    def _apply_period(self, key):
        today = datetime.date.today()
        if key == '7d':
            cutoff = (today - datetime.timedelta(days=7)).isoformat()
        elif key == '30d':
            cutoff = (today - datetime.timedelta(days=30)).isoformat()
        elif key == '90d':
            cutoff = (today - datetime.timedelta(days=90)).isoformat()
        else:
            cutoff = None

        filtered = [t for t in self._all_txns
                    if cutoff is None or (t.get('posted_date') or '') >= cutoff]

        total_in  = sum((t.get('amount_minor') or 0) for t in filtered
                        if t.get('transaction_type') in ('deposit',))
        total_out = sum((t.get('amount_minor') or 0) for t in filtered
                        if t.get('transaction_type') in ('withdrawal', 'loan_repayment'))

        self._total_in_card._v.text  = f"KSh {total_in/100:,.0f}"
        self._total_out_card._v.text = f"KSh {total_out/100:,.0f}"
        self._txn_count_card._v.text = str(len(filtered))

        self._txn_list.clear_widgets()
        if not filtered:
            self._txn_list.add_widget(MDLabel(
                text='No transactions in this period.',
                halign='center', theme_text_color='Secondary',
                size_hint_y=None, height=dp(50), valign='middle'
            ))
            return

        for tx in filtered:
            self._txn_list.add_widget(self._txn_row(tx))

    def _txn_row(self, tx):
        ttype = tx.get('transaction_type', '')
        is_credit = ttype == 'deposit'
        color = 'success' if is_credit else 'error'
        icon  = 'arrow-down-circle' if is_credit else 'arrow-up-circle'

        card = MDCard(
            orientation='horizontal',
            padding=[dp(12), dp(8)], spacing=dp(10),
            radius=[dp(10)],
            md_bg_color=get_color('surface_variant', 0.1),
            size_hint_y=None, height=dp(60), elevation=0
        )
        ic_rl = RelativeLayout(size_hint=(None, None), size=(dp(36), dp(36)))
        with ic_rl.canvas.before:
            Color(*get_color(f'{color}_container', 0.45))
            RoundedRectangle(pos=(0, 0), size=(dp(36), dp(36)), radius=[dp(18)])
        ic_rl.add_widget(MDIcon(
            icon=icon, theme_text_color='Custom', text_color=get_color(color),
            halign='center', valign='middle', font_size=sp(18),
            size_hint=(None, None), size=(dp(22), dp(22)),
            pos_hint={'center_x': 0.5, 'center_y': 0.5}
        ))
        card.add_widget(ic_rl)

        info = MDBoxLayout(orientation='vertical', spacing=dp(2))
        info.add_widget(MDLabel(
            text=ttype.replace('_', ' ').title(),
            font_style='Subtitle2', size_hint_y=None, height=dp(22), valign='middle'
        ))
        date_str = (tx.get('posted_date') or tx.get('created_at') or '')[:10]
        ref = tx.get('reference_no') or tx.get('id', '')[:8]
        info.add_widget(MDLabel(
            text=f"{date_str}  •  Ref: {ref}",
            font_style='Caption', theme_text_color='Secondary',
            size_hint_y=None, height=dp(18), valign='middle'
        ))
        card.add_widget(info)

        amt = (tx.get('amount_minor') or 0) / 100
        sign = '+' if is_credit else '-'
        card.add_widget(MDLabel(
            text=f"{sign}KSh {abs(amt):,.2f}",
            font_style='Subtitle2', bold=True,
            halign='right', valign='middle',
            theme_text_color='Custom',
            text_color=get_color(color)
        ))
        return card

    def _export(self):
        self.show_info('Statement export coming soon')


# ============================================================================
# LOAN CALCULATOR SCREEN
# ============================================================================

class LoanCalculatorScreen(BaseScreen):
    """EMI / amortisation loan calculator — works offline, no DB needed."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = 'loan_calculator'
        self._build()

    def _build(self):
        from kivy.uix.floatlayout import FloatLayout
        float_root = FloatLayout()
        root = MDBoxLayout(orientation='vertical', size_hint=(1, 1))

        toolbar = MDTopAppBar(
            title='Loan Calculator',
            elevation=2,
            md_bg_color=get_color('quaternary'),
            specific_text_color=(1, 1, 1, 1),
            left_action_items=[['arrow-left', lambda x: self.app.navigate_back()]],
        )
        root.add_widget(toolbar)

        scroll = MDScrollView(size_hint=(1, 1))
        content = MDBoxLayout(
            orientation='vertical', spacing=dp(14),
            padding=[dp(16), dp(16), dp(16), dp(24)],
            size_hint_y=None
        )
        content.bind(minimum_height=content.setter('height'))

        # ── Input card ─────────────────────────────────────────────
        input_card = MDCard(
            orientation='vertical',
            padding=dp(16), spacing=dp(10),
            radius=[dp(14)],
            md_bg_color=get_color('surface_variant', 0.15),
            size_hint_y=None, elevation=1
        )
        input_card.bind(minimum_height=input_card.setter('height'))

        input_card.add_widget(MDLabel(
            text='Loan Details', font_style='Subtitle1', bold=True,
            theme_text_color='Custom', text_color=get_color('quaternary'),
            size_hint_y=None, height=dp(28), valign='middle'
        ))

        self._amount_field = MDTextField(
            hint_text='Loan Amount (KSh)', mode='rectangle',
            input_filter='float', size_hint_y=None, height=dp(56)
        )
        self._rate_field = MDTextField(
            hint_text='Annual Interest Rate (%)', mode='rectangle',
            input_filter='float', size_hint_y=None, height=dp(56),
            text='18'
        )
        self._term_field = MDTextField(
            hint_text='Loan Term (months)', mode='rectangle',
            input_filter='int', size_hint_y=None, height=dp(56),
            text='12'
        )

        # Repayment method selector
        method_row = MDBoxLayout(size_hint_y=None, height=dp(40), spacing=dp(8))
        self._method = 'reducing'
        self._method_btns = {}
        for key, label in [('reducing', 'Reducing Balance'), ('flat', 'Flat Rate')]:
            active = key == 'reducing'
            btn = MDCard(
                size_hint=(1, None), height=dp(36), radius=[dp(18)],
                md_bg_color=get_color('quaternary') if active else get_color('surface_variant', 0.4),
                ripple_behavior=True,
                on_release=lambda x, k=key: self._set_method(k)
            )
            lbl = MDLabel(
                text=label, halign='center', valign='middle',
                font_style='Caption', bold=active,
                theme_text_color='Custom',
                text_color=(1, 1, 1, 1) if active else get_color('on_surface')
            )
            btn.add_widget(lbl)
            self._method_btns[key] = (btn, lbl)
            method_row.add_widget(btn)

        for w in [self._amount_field, self._rate_field, self._term_field, method_row]:
            input_card.add_widget(w)

        calc_btn = MDRaisedButton(
            text='CALCULATE', md_bg_color=get_color('quaternary'),
            size_hint_y=None, height=dp(48),
            on_release=lambda x: self._calculate()
        )
        input_card.add_widget(calc_btn)
        content.add_widget(input_card)

        # ── Result cards ───────────────────────────────────────────
        self._result_grid = MDGridLayout(
            cols=2, spacing=dp(10), size_hint_y=None, height=dp(0)
        )
        content.add_widget(self._result_grid)

        # ── Schedule section ───────────────────────────────────────
        self._schedule_label = MDLabel(
            text='', font_style='Caption', theme_text_color='Secondary',
            bold=True, size_hint_y=None, height=dp(0), valign='middle'
        )
        content.add_widget(self._schedule_label)

        self._schedule_box = MDBoxLayout(
            orientation='vertical', spacing=dp(4), size_hint_y=None
        )
        self._schedule_box.bind(minimum_height=self._schedule_box.setter('height'))
        content.add_widget(self._schedule_box)

        scroll.add_widget(content)
        root.add_widget(scroll)
        float_root.add_widget(root)
        self.add_widget(float_root)

    def _set_method(self, key):
        self._method = key
        for k, (btn, lbl) in self._method_btns.items():
            active = k == key
            btn.md_bg_color = get_color('quaternary') if active else get_color('surface_variant', 0.4)
            lbl.text_color  = (1, 1, 1, 1) if active else get_color('on_surface')
            lbl.bold = active

    def _calculate(self):
        try:
            P = float(self._amount_field.text or 0)
            r = float(self._rate_field.text or 0) / 100
            n = int(self._term_field.text or 12)
            if P <= 0 or n <= 0:
                self.show_error('Enter a valid amount and term.')
                return

            if self._method == 'reducing':
                monthly_r = r / 12
                if monthly_r == 0:
                    emi = P / n
                else:
                    emi = P * monthly_r * (1 + monthly_r)**n / ((1 + monthly_r)**n - 1)
                total_pay = emi * n
                total_int = total_pay - P

                # Generate schedule
                schedule = []
                balance = P
                for i in range(1, n + 1):
                    interest = balance * monthly_r
                    principal_part = emi - interest
                    balance -= principal_part
                    schedule.append({
                        'month': i, 'emi': emi,
                        'principal': principal_part,
                        'interest': interest,
                        'balance': max(balance, 0)
                    })
            else:
                # Flat rate
                total_int = P * r * (n / 12)
                total_pay = P + total_int
                emi = total_pay / n
                schedule = []
                monthly_int = total_int / n
                monthly_princ = P / n
                balance = P
                for i in range(1, n + 1):
                    balance -= monthly_princ
                    schedule.append({
                        'month': i, 'emi': emi,
                        'principal': monthly_princ,
                        'interest': monthly_int,
                        'balance': max(balance, 0)
                    })

            self._render_results(P, emi, total_int, total_pay, n, schedule)
        except Exception as e:
            self.show_error(f'Calculation error: {e}')

    def _render_results(self, principal, emi, total_int, total_pay, n, schedule):
        # Result summary cards
        self._result_grid.clear_widgets()
        self._result_grid.height = dp(280)
        for value, label, color in [
            (f"KSh {emi:,.2f}",      'Monthly EMI',       'quaternary'),
            (f"KSh {principal:,.2f}",'Loan Amount',        'primary'),
            (f"KSh {total_int:,.2f}",'Total Interest',     'error'),
            (f"KSh {total_pay:,.2f}",'Total Repayable',    'secondary'),
            (f"{total_int/principal*100:.1f}%", 'Interest Rate Eff.', 'warning'),
            (f"{n} months",          'Term',               'tertiary'),
        ]:
            card = MDCard(
                orientation='vertical', padding=[dp(10), dp(8)],
                radius=[dp(12)],
                md_bg_color=get_color(f'{color}_container', 0.25),
                elevation=0
            )
            card.add_widget(MDLabel(
                text=value, font_style='Subtitle1', bold=True,
                theme_text_color='Custom', text_color=get_color(color),
                halign='center', valign='middle',
                size_hint_y=None, height=dp(28)
            ))
            card.add_widget(MDLabel(
                text=label, font_style='Caption',
                theme_text_color='Secondary',
                halign='center', valign='middle',
                size_hint_y=None, height=dp(18)
            ))
            self._result_grid.add_widget(card)

        # Amortisation schedule header
        self._schedule_label.text = 'REPAYMENT SCHEDULE'
        self._schedule_label.height = dp(26)

        self._schedule_box.clear_widgets()
        # Column header
        hdr = MDBoxLayout(size_hint_y=None, height=dp(28))
        for txt, weight in [('Mo.', 0.1), ('EMI', 0.25), ('Principal', 0.25),
                             ('Interest', 0.22), ('Balance', 0.18)]:
            hdr.add_widget(MDLabel(
                text=txt, font_style='Caption', bold=True,
                theme_text_color='Secondary',
                halign='right' if weight < 0.2 else 'right',
                size_hint_x=weight, valign='middle',
                size_hint_y=None, height=dp(28)
            ))
        self._schedule_box.add_widget(hdr)

        for row in schedule:
            r = MDBoxLayout(
                size_hint_y=None, height=dp(24),
                md_bg_color=get_color('surface_variant', 0.08) if row['month'] % 2 == 0 else (0, 0, 0, 0)
            )
            for val, weight in [
                (str(row['month']),             0.10),
                (f"{row['emi']:,.0f}",          0.25),
                (f"{row['principal']:,.0f}",    0.25),
                (f"{row['interest']:,.0f}",     0.22),
                (f"{row['balance']:,.0f}",      0.18),
            ]:
                r.add_widget(MDLabel(
                    text=val, font_style='Caption',
                    halign='right', valign='middle',
                    size_hint_x=weight,
                    size_hint_y=None, height=dp(24)
                ))
            self._schedule_box.add_widget(r)


# ============================================================================
# MY PROFILE SCREEN  (member's own profile — read only + request edit)
# ============================================================================

class MyProfileScreen(BaseScreen):
    """Member's own profile screen with account summary and quick actions."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = 'my_profile'
        self._build()

    def _build(self):
        root = MDBoxLayout(orientation='vertical')
        toolbar = MDTopAppBar(
            title='My Profile',
            elevation=2, md_bg_color=get_color('primary'),
            specific_text_color=(1, 1, 1, 1),
            left_action_items=[['arrow-left', lambda x: self.app.navigate_back()]],
        )
        root.add_widget(toolbar)

        scroll = MDScrollView(size_hint=(1, 1))
        content = MDBoxLayout(
            orientation='vertical', spacing=dp(12),
            padding=[dp(14), dp(14), dp(14), dp(24)],
            size_hint_y=None
        )
        content.bind(minimum_height=content.setter('height'))

        # Avatar + name hero
        self._hero = MDCard(
            orientation='vertical',
            padding=[dp(20), dp(16)], spacing=dp(4),
            radius=[dp(16)], md_bg_color=get_color('primary'),
            size_hint_y=None, height=dp(130), elevation=3
        )
        av_row = MDBoxLayout(size_hint_y=None, height=dp(60), spacing=dp(14))
        self._av_rl = RelativeLayout(size_hint=(None, None), size=(dp(60), dp(60)))
        with self._av_rl.canvas.before:
            Color(1, 1, 1, 0.22)
            RoundedRectangle(pos=(0, 0), size=(dp(60), dp(60)), radius=[dp(30)])
        self._initials_lbl = MDLabel(
            text='?', halign='center', valign='middle',
            font_style='H5', bold=True,
            theme_text_color='Custom', text_color=(1, 1, 1, 1),
            size_hint=(None, None), size=(dp(60), dp(60)),
            pos_hint={'center_x': 0.5, 'center_y': 0.5}
        )
        self._av_rl.add_widget(self._initials_lbl)
        av_row.add_widget(self._av_rl)
        name_col = MDBoxLayout(orientation='vertical', spacing=dp(2))
        self._name_lbl = MDLabel(
            text='—', font_style='H6', bold=True,
            theme_text_color='Custom', text_color=(1, 1, 1, 1),
            size_hint_y=None, height=dp(30), valign='middle'
        )
        self._memno_lbl = MDLabel(
            text='—', font_style='Caption',
            theme_text_color='Custom', text_color=(1, 1, 1, 0.75),
            size_hint_y=None, height=dp(20), valign='middle'
        )
        self._kyc_lbl = MDLabel(
            text='KYC: Pending', font_style='Caption',
            theme_text_color='Custom', text_color=(1, 1, 1, 0.65),
            size_hint_y=None, height=dp(18), valign='middle'
        )
        for w in [self._name_lbl, self._memno_lbl, self._kyc_lbl]:
            name_col.add_widget(w)
        av_row.add_widget(name_col)
        self._hero.add_widget(av_row)
        content.add_widget(self._hero)

        # Account balance card
        self._bal_card = MDCard(
            orientation='horizontal',
            padding=[dp(16), dp(12)], spacing=dp(12),
            radius=[dp(14)],
            md_bg_color=get_color('tertiary_container', 0.3),
            size_hint_y=None, height=dp(70), elevation=1
        )
        bal_icon = RelativeLayout(size_hint=(None, None), size=(dp(44), dp(44)))
        with bal_icon.canvas.before:
            Color(*get_color('tertiary_container', 0.6))
            RoundedRectangle(pos=(0, 0), size=(dp(44), dp(44)), radius=[dp(12)])
        bal_icon.add_widget(MDIcon(
            icon='bank', theme_text_color='Custom', text_color=get_color('tertiary'),
            halign='center', valign='middle', font_size=sp(22),
            size_hint=(None, None), size=(dp(26), dp(26)),
            pos_hint={'center_x': 0.5, 'center_y': 0.5}
        ))
        self._bal_card.add_widget(bal_icon)
        bal_info = MDBoxLayout(orientation='vertical', spacing=dp(2))
        self._bal_lbl = MDLabel(
            text='KSh 0.00', font_style='H6', bold=True,
            theme_text_color='Custom', text_color=get_color('tertiary'),
            size_hint_y=None, height=dp(28), valign='middle'
        )
        bal_info.add_widget(self._bal_lbl)
        bal_info.add_widget(MDLabel(
            text='Savings Account Balance',
            font_style='Caption', theme_text_color='Secondary',
            size_hint_y=None, height=dp(18), valign='middle'
        ))
        self._bal_card.add_widget(bal_info)
        content.add_widget(self._bal_card)

        # Quick action buttons
        actions_row = MDBoxLayout(size_hint_y=None, height=dp(44), spacing=dp(8))
        for label, color, screen in [
            ('View Statement', 'primary',   'statement'),
            ('Calculator',     'quaternary','loan_calculator'),
            ('Investments',    'info',      'investments'),
        ]:
            btn = MDRaisedButton(
                text=label, md_bg_color=get_color(color),
                size_hint_x=1, size_hint_y=None, height=dp(40),
                on_release=lambda x, s=screen: self.app.navigate_to(s)
            )
            actions_row.add_widget(btn)
        content.add_widget(actions_row)

        # Personal details
        self._details_box = MDBoxLayout(
            orientation='vertical', spacing=dp(6), size_hint_y=None
        )
        self._details_box.bind(minimum_height=self._details_box.setter('height'))
        content.add_widget(self._details_box)

        scroll.add_widget(content)
        root.add_widget(scroll)
        self.add_widget(root)

    def on_enter(self):
        threading.Thread(target=self._load, daemon=True).start()

    def _load(self):
        try:
            user = self.app.db.fetch_one(
                "SELECT member_id FROM users WHERE id=?", (self.app.current_user_id,))
            mid = (user or {}).get('member_id')
            if not mid:
                return
            member = self.app.db.fetch_one("SELECT * FROM members WHERE id=?", (mid,))
            acc = self.app.db.fetch_one(
                "SELECT * FROM accounts WHERE member_id=? AND account_type='savings'", (mid,))
            Clock.schedule_once(lambda dt: self._render(member, acc), 0)
        except Exception as e:
            Logger.error(f'MyProfile load: {e}')

    def _render(self, member, acc):
        if not member:
            return
        fn = member.get('first_name', '')
        ln = member.get('last_name', '')
        initials = ((fn[0] if fn else '') + (ln[0] if ln else '')).upper() or '?'
        self._initials_lbl.text = initials
        self._name_lbl.text = f"{fn} {ln}"
        self._memno_lbl.text = member.get('member_no', '—')
        kyc = member.get('kyc_status', 'pending') or 'pending'
        self._kyc_lbl.text = f"KYC: {kyc.title()}"

        bal = (acc.get('balance_minor') or 0) / 100 if acc else 0
        self._bal_lbl.text = f"KSh {bal:,.2f}"

        # Personal details card
        self._details_box.clear_widgets()
        fields = [
            ('Phone',        member.get('phone', '—')),
            ('Email',        member.get('email', '—')),
            ('ID Number',    member.get('id_number', '—')),
            ('Date of Birth',member.get('date_of_birth', '—')),
            ('Gender',       member.get('gender', '—')),
            ('Occupation',   member.get('occupation', '—')),
            ('Joined',       member.get('membership_date', '—')),
        ]
        card = MDCard(
            orientation='vertical', padding=dp(14), spacing=dp(2),
            radius=[dp(12)],
            md_bg_color=get_color('surface_variant', 0.15),
            size_hint_y=None, elevation=0
        )
        card.add_widget(MDLabel(
            text='Personal Information', font_style='Subtitle1', bold=True,
            theme_text_color='Custom', text_color=get_color('primary'),
            size_hint_y=None, height=dp(28), valign='middle'
        ))
        for label, value in fields:
            row = MDBoxLayout(size_hint_y=None, height=dp(28))
            row.add_widget(MDLabel(
                text=label, font_style='Caption',
                theme_text_color='Secondary', size_hint_x=0.38,
                size_hint_y=None, height=dp(28), valign='middle'
            ))
            row.add_widget(MDLabel(
                text=str(value or '—'), font_style='Body2',
                size_hint_x=0.62,
                size_hint_y=None, height=dp(28), valign='middle'
            ))
            card.add_widget(row)
        card.height = dp(14 + 28 + sum(28 for _, v in fields) + 14)
        self._details_box.add_widget(card)
