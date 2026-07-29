from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import StyleSheet1, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, HRFlowable
from pypdf import PdfReader
import pdfplumber
from pathlib import Path

ROOT = Path(r"D:\2026\MLP\COMPICK")
OUT = ROOT / "output" / "pdf" / "feature-guides"
OUT.mkdir(parents=True, exist_ok=True)

pdfmetrics.registerFont(TTFont("Han", r"C:\WINDOWS\Fonts\HANDOTUM.TTF"))
pdfmetrics.registerFont(TTFont("HanB", r"C:\WINDOWS\Fonts\HANDOTUMB.TTF"))

NAVY = colors.HexColor("#172033")
BLUE = colors.HexColor("#2563EB")
SOFT = colors.HexColor("#F4F7FC")
LINE = colors.HexColor("#D5DEED")
MUTED = colors.HexColor("#64748B")
GREEN = colors.HexColor("#15803D")
AMBER = colors.HexColor("#D97706")
RED = colors.HexColor("#C9362B")

s = StyleSheet1()
s.add(ParagraphStyle(name="Title", fontName="HanB", fontSize=25, leading=34, textColor=NAVY, spaceAfter=8))
s.add(ParagraphStyle(name="Sub", fontName="Han", fontSize=11, leading=18, textColor=MUTED))
s.add(ParagraphStyle(name="H1", fontName="HanB", fontSize=20, leading=27, textColor=NAVY, spaceAfter=12))
s.add(ParagraphStyle(name="H2", fontName="HanB", fontSize=13.5, leading=20, textColor=BLUE, spaceBefore=8, spaceAfter=6))
s.add(ParagraphStyle(name="Body", fontName="Han", fontSize=9, leading=15, textColor=NAVY, spaceAfter=5))
s.add(ParagraphStyle(name="Small", fontName="Han", fontSize=7.6, leading=12, textColor=MUTED))
s.add(ParagraphStyle(name="Bullet", fontName="Han", fontSize=8.9, leading=14.5, leftIndent=12, firstLineIndent=-8, textColor=NAVY, spaceAfter=4))
s.add(ParagraphStyle(name="Code", fontName="Han", fontSize=7.2, leading=11.4, leftIndent=7, rightIndent=7, borderColor=LINE, borderWidth=.5, borderPadding=7, backColor=SOFT, textColor=NAVY, spaceAfter=7))
s.add(ParagraphStyle(name="TH", fontName="HanB", fontSize=8, leading=11.5, textColor=colors.white))
s.add(ParagraphStyle(name="TD", fontName="Han", fontSize=7.7, leading=11.8, textColor=NAVY))
s.add(ParagraphStyle(name="Tag", fontName="HanB", fontSize=7.2, leading=10, textColor=colors.white))

def P(x, sty="Body"):
    return Paragraph(x, s[sty])

def B(x):
    return P("• " + x, "Bullet")

def T(rows, widths, header=True):
    data = [[P(str(v), "TH" if header and i == 0 else "TD") for v in row] for i, row in enumerate(rows)]
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    cmd = [
        ("VALIGN", (0,0), (-1,-1), "TOP"), ("GRID", (0,0), (-1,-1), .35, LINE),
        ("LEFTPADDING", (0,0), (-1,-1), 7), ("RIGHTPADDING", (0,0), (-1,-1), 7),
        ("TOPPADDING", (0,0), (-1,-1), 6), ("BOTTOMPADDING", (0,0), (-1,-1), 6),
    ]
    if header:
        cmd += [("BACKGROUND", (0,0), (-1,0), NAVY), ("ROWBACKGROUNDS", (0,1), (-1,-1), [colors.white, SOFT])]
    t.setStyle(TableStyle(cmd))
    return t

def note(label, text, color=BLUE):
    tag = Table([[P(label, "Tag")]], colWidths=[26*mm], rowHeights=[7*mm])
    tag.setStyle(TableStyle([("BACKGROUND", (0,0),(-1,-1),color), ("VALIGN",(0,0),(-1,-1),"MIDDLE")]))
    box = Table([[tag, P(text)]], colWidths=[30*mm, 132*mm])
    box.setStyle(TableStyle([
        ("BOX", (0,0),(-1,-1),.6,LINE), ("BACKGROUND",(0,0),(-1,-1),colors.HexColor("#F9FBFF")),
        ("VALIGN",(0,0),(-1,-1),"MIDDLE"), ("LEFTPADDING",(0,0),(-1,-1),8),
        ("RIGHTPADDING",(0,0),(-1,-1),8), ("TOPPADDING",(0,0),(-1,-1),8), ("BOTTOMPADDING",(0,0),(-1,-1),8)
    ]))
    return box

def page_head(no, title, sub):
    return [P(f"{no:02d}  {title}", "H1"), P(sub, "Sub"), Spacer(1,4*mm), HRFlowable(width="100%", thickness=1, color=LINE), Spacer(1,5*mm)]

def hf(title):
    def draw(canvas, doc):
        canvas.saveState()
        if doc.page > 1:
            canvas.setFont("Han", 7.3); canvas.setFillColor(MUTED)
            canvas.drawString(20*mm, 286*mm, "COMPICK 기능별 코드 설명")
            canvas.drawRightString(190*mm, 286*mm, title)
            canvas.setStrokeColor(LINE); canvas.line(20*mm,282.5*mm,190*mm,282.5*mm)
            canvas.drawCentredString(105*mm, 10*mm, str(doc.page))
        canvas.restoreState()
    return draw

def cover(title, subtitle, files, purpose):
    return [Spacer(1,28*mm), P("COMPICK CODE GUIDE", "Sub"), Spacer(1,3*mm), P(title, "Title"), P(subtitle, "Sub"),
            Spacer(1,16*mm), HRFlowable(width="100%", thickness=3, color=BLUE), Spacer(1,10*mm),
            T([["구분", "내용"], ["문서 목적", purpose], ["관련 파일", "<br/>".join(files)], ["기준", "2026-07-29 현재 워크스페이스"], ["기술", "Spring Boot 3.5.16 / Java 21 / Thymeleaf / Spring Security / JPA"]], [29*mm,133*mm]),
            Spacer(1,12*mm), note("읽는 순서", "요청 흐름 → 파일 책임 → 핵심 코드 → 검증·주의사항 순으로 구성했다.", GREEN), PageBreak()]

def build(filename, title, subtitle, files, purpose, pages, required):
    path = OUT / filename
    story = cover(title, subtitle, files, purpose)
    for idx, page in enumerate(pages):
        story += page
        if idx != len(pages)-1:
            story.append(PageBreak())
    doc = SimpleDocTemplate(str(path), pagesize=A4, leftMargin=20*mm, rightMargin=20*mm, topMargin=19*mm, bottomMargin=17*mm,
                            title=title, author="OpenAI Codex")
    doc.build(story, onFirstPage=hf(title), onLaterPages=hf(title))
    reader = PdfReader(str(path))
    with pdfplumber.open(str(path)) as pdf:
        text = "\n".join((p.extract_text() or "") for p in pdf.pages)
    for item in required:
        assert item in text, f"{filename}: missing {item}"
    assert len(reader.pages) >= 4
    return path, len(reader.pages)

docs = []

# 00 overview
docs.append(build(
    "00_COMPICK_전체_구조_가이드.pdf", "00 전체 구조 가이드", "회원 서비스 코드를 처음 읽는 사람을 위한 지도",
    ["com.boot.compick.member.controller", "com.boot.compick.member.service", "com.boot.compick.member.repository", "com.boot.compick.member.entity", "resources/templates/member"],
    "패키지 간 책임과 한 요청이 처리되는 전체 경로를 이해한다.",
    [
        page_head(1,"계층 구조","Controller부터 DB와 화면까지") + [
            T([["계층","하는 일","예시"],["Controller","HTTP 입력을 받고 화면 또는 리다이렉트를 결정","MemberController, RecoveryController"],["Service","업무 규칙과 트랜잭션을 실행","MemberService, AddressService"],["Repository","JPA 쿼리로 DB에 접근","MemberRepository"],["Entity","테이블 데이터와 상태 변경을 표현","Member, Address"],["DTO","화면 입력값과 유효성 검증 규칙 보유","JoinForm, ProfileForm"],["Template","Thymeleaf로 서버 데이터를 HTML로 렌더링","join.html, mypage.html"]],[30*mm,72*mm,60*mm]),
            Spacer(1,7*mm), P("기본 호출 구조","H2"), P("브라우저 → Controller → Service → Repository → Oracle", "Code"),
            B("Controller는 Repository를 직접 호출하지 않아 역할 경계가 분명하다."), B("변경 작업은 Service의 @Transactional 메서드 안에서 수행된다."), B("화면 입력은 DTO의 Bean Validation으로 1차 검사된다.")],
        page_head(2,"패키지 지도","기능을 찾을 때 어디부터 볼 것인가") + [
            T([["찾고 싶은 기능","시작 파일","다음으로 볼 파일"],["회원가입·로그인","MemberController","JoinForm → MemberService → Member"],["Google 로그인","SecurityConfig","CompickOidcUserService → SocialAccountService"],["아이디·비밀번호 찾기","RecoveryController","EmailVerificationService → VerificationMailService"],["마이페이지","MemberController","MemberService → Member"],["배송지","AddressController","AddressService → AddressRepository → Address"],["접근 권한","SecurityConfig","MemberUserDetailsService"],["화면 스타일","templates/member","static/css/member.css"]],[41*mm,52*mm,69*mm]),
            Spacer(1,8*mm), note("팁","오류가 화면 입력과 관련되면 DTO→Controller, DB 값과 관련되면 Service→Entity→Repository 순서로 추적하면 빠르다.",BLUE)],
        page_head(3,"공통 실행 원리","인증, 검증, 트랜잭션") + [
            P("인증","H2"), B("Spring Security가 로그인 세션과 보호 경로 접근을 관리한다."), B("Authentication.getName()은 현재 회원의 loginId이며 서비스 조회 키로 사용된다."),
            P("검증","H2"), B("@Valid가 DTO 애너테이션을 검사하고 BindingResult가 화면 오류를 보관한다."), B("중복 아이디, 현재 비밀번호 일치 같은 DB 의존 규칙은 Service가 검사한다."),
            P("트랜잭션","H2"), B("조회 서비스는 @Transactional(readOnly=true)를 기본으로 사용한다."), B("회원 저장, 비밀번호 변경, 배송지 저장 등에는 @Transactional을 별도로 적용한다."),
            P("화면 반환","H2"), P("return \"member/join\";      // 템플릿을 바로 렌더링<br/>return \"redirect:/\";        // 새 요청으로 메인 페이지 이동", "Code"),
            note("핵심","컨트롤러의 반환 문자열이 템플릿 이름인지 redirect인지 먼저 구분하면 흐름을 이해하기 쉽다.",GREEN)],
    ], ["Controller", "Service", "Repository", "Authentication"]
))

# 01 local auth
docs.append(build(
    "01_로컬_회원가입_로그인_코드설명.pdf", "01 로컬 회원가입·로그인", "아이디와 비밀번호 기반 계정 생성 및 인증",
    ["member/controller/MemberController.java", "member/dto/JoinForm.java", "member/service/MemberService.java", "member/service/MemberUserDetailsService.java", "member/entity/Member.java", "templates/member/join.html", "templates/member/login.html"],
    "회원가입 입력부터 BCrypt 저장, 자동 로그인, 일반 로그인까지 추적한다.",
    [
        page_head(1,"회원가입 흐름","POST /member/join 처리 순서") + [
            P("1. JoinForm","H2"), B("loginId는 영문·숫자 4~20자, password는 영문·숫자·특수문자 포함 8자 이상이다."), B("이메일 형식, 이름, 휴대전화, 약관 동의를 함께 검증한다."),
            P("2. MemberController.join","H2"), B("비밀번호 확인값을 비교하고 BindingResult에 오류를 추가한다."), B("오류가 없으면 MemberService.join을 호출한다."), B("가입 완료 후 signIn으로 인증 세션을 저장한다."),
            P("3. MemberService.join","H2"), B("loginId와 email 중복을 확인한다."), B("PasswordEncoder로 비밀번호를 BCrypt 해시한 뒤 Member를 저장한다."),
            P("핵심 코드","H2"), P("memberRepository.save(new Member(<br/>    form.getLoginId(),<br/>    passwordEncoder.encode(form.getPassword()),<br/>    form.getName(), form.getEmail(), form.getPhone()));", "Code")],
        page_head(2,"로그인 흐름","Spring Security가 폼 요청을 처리하는 방법") + [
            T([["설정","값","의미"],["로그인 화면","/member/login","GET 화면은 직접 만든 템플릿 사용"],["처리 URL","/member/login","POST는 Security 필터가 처리"],["아이디 파라미터","loginId","기본 username 대신 프로젝트 필드 사용"],["비밀번호 파라미터","password","PasswordEncoder로 해시 비교"],["성공 이동","/","메인 페이지"],["실패 이동","/member/login?error","로그인 오류 안내"]],[36*mm,47*mm,79*mm]),
            Spacer(1,8*mm), P("MemberUserDetailsService","H2"), B("입력된 loginId로 Member를 조회한다."), B("저장된 passwordHash와 role을 Spring Security UserDetails로 변환한다."), B("WITHDRAWN 회원은 disabled=true가 되어 인증되지 않는다."),
            P("자동 로그인 보조 메서드","H2"), P("Authentication authentication = authenticationManager.authenticate(...);<br/>context.setAuthentication(authentication);<br/>securityContextRepository.saveContext(context, request, response);", "Code")],
        page_head(3,"데이터와 예외","MEMBER에 무엇이 저장되는가") + [
            T([["필드","회원가입 값","설명"],["LOGIN_ID","사용자 입력","로그인 키, unique"],["PASSWORD_HASH","BCrypt 결과","원문 비밀번호 저장 금지"],["MEMBER_NAME","사용자 입력","마이페이지 표시"],["EMAIL","사용자 입력","unique, 계정 복구에 사용"],["PHONE","사용자 입력","회원정보에서 변경 가능"],["MEMBER_ROLE","USER","권한"],["MEMBER_STATUS","ACTIVE","탈퇴 시 WITHDRAWN"]],[37*mm,45*mm,80*mm]),
            Spacer(1,8*mm), note("개선 필요","로컬 가입 이메일은 현재 입력값 그대로 중복 검사·저장한다. Google·복구 흐름과 동일하게 trim 후 소문자화하는 공통 규칙이 필요하다.",AMBER),
            Spacer(1,6*mm), P("오류가 발생하면","H2"), B("DTO 오류는 각 입력칸 아래 th:errors로 표시된다."), B("중복 아이디·이메일은 Service의 IllegalArgumentException 메시지를 전역 오류로 표시한다."), B("DB unique 제약도 최종 중복 방어선 역할을 한다.")],
    ], ["BCrypt", "MemberUserDetailsService", "PASSWORD_HASH", "signIn"]
))

# 02 google
docs.append(build(
    "02_Google_OAuth_코드설명.pdf", "02 Google OAuth", "Google 인증과 COMPICK 회원 계정 연결",
    ["common/config/GoogleOAuthConfig.java", "common/config/SecurityConfig.java", "member/service/CompickOidcUserService.java", "member/service/SocialAccountService.java", "member/security/CompickOidcUser.java", "member/dto/SocialCredentialForm.java", "templates/member/social-credentials.html"],
    "Google OIDC 콜백부터 신규 회원의 아이디·비밀번호 설정까지 이해한다.",
    [
        page_head(1,"인증 시작과 콜백","환경변수와 Spring Security") + [
            P("GoogleOAuthConfig","H2"), B("GOOGLE_CLIENT_ID와 GOOGLE_CLIENT_SECRET가 모두 있을 때만 ClientRegistrationRepository를 생성한다."), B("scope는 openid, profile, email이다."),
            P("SecurityConfig","H2"), B("/oauth2/authorization/google에서 Google 인증을 시작한다."), B("콜백 후 CompickOidcUserService가 사용자 정보를 처리한다."), B("신규 소셜 회원이면 /member/social-credentials, 기존 회원이면 /로 보낸다."),
            P("성공 분기","H2"), P("credentialSetupRequired == true<br/>    → /member/social-credentials<br/>credentialSetupRequired == false<br/>    → /", "Code"),
            note("조건부 기능","Google 환경변수가 없으면 Google 로그인 버튼과 OAuth 설정이 비활성화되지만 로컬 로그인은 정상 기동한다.",GREEN)],
        page_head(2,"사용자 로딩","CompickOidcUserService와 계정 연결") + [
            P("CompickOidcUserService.loadUser","H2"), B("기본 OidcUserService로 Google 사용자 정보를 읽는다."), B("email_verified가 true이고 email이 존재하는지 확인한다."), B("Google subject, email, fullName을 SocialAccountService로 전달한다."),
            P("SocialAccountService.loginGoogleWithResult","H2"),
            T([["상황","처리"],["동일 provider_user_id 존재","연결된 Member 재사용, 제공자 이메일 갱신"],["연결은 없지만 동일 email 회원 존재","기존 로컬 Member에 Google 계정 연결"],["둘 다 없음","임시 loginId와 무작위 비밀번호로 Member 생성 후 연결"]],[54*mm,108*mm]),
            Spacer(1,7*mm), P("SOCIAL_ACCOUNT","H2"), B("provider=GOOGLE, providerUserId=Google subject, providerEmail을 저장한다."), B("provider + provider_user_id 조합은 unique이므로 한 Google 계정이 중복 연결되지 않는다.")],
        page_head(3,"최초 아이디·비밀번호 설정","임시 계정을 최종 계정으로 바꾸는 과정") + [
            P("임시 상태","H2"), B("신규 Google 회원의 loginId는 google_ + subject 형태로 만들어진다."), B("비밀번호는 UUID를 해시하여 사용자가 알 수 없는 값으로 둔다."),
            P("설정 화면","H2"), B("SocialCredentialForm이 아이디·비밀번호·확인을 검증한다."), B("아이디 중복 확인 API는 /member/check-login-id를 재사용한다."), B("setLoginCredentials가 loginId와 passwordHash를 함께 갱신한다."), B("저장 후 일반 로그인 Authentication으로 교체해 Google과 로컬 로그인을 모두 사용할 수 있다."),
            P("완료 후 재로그인","H2"), P("Google 로그인 → 기존 SOCIAL_ACCOUNT 조회 → MEMBER.loginId 확인 → 설정 완료면 메인 이동", "Code"),
            note("구조 개선","현재 loginId가 google_로 시작하는지를 설정 미완료 표시로 사용한다. 장기적으로는 ACCOUNT_SETUP_STATUS 같은 명시적 컬럼이 안전하다.",AMBER)],
    ], ["email_verified", "SOCIAL_ACCOUNT", "credentialSetupRequired", "social-credentials"]
))

# 03 recovery
docs.append(build(
    "03_아이디찾기_비밀번호재설정_코드설명.pdf", "03 계정 찾기·비밀번호 재설정", "이메일 인증 코드 기반 계정 복구",
    ["member/controller/RecoveryController.java", "member/service/EmailVerificationService.java", "member/service/VerificationMailService.java", "member/service/MemberService.java", "member/entity/EmailVerification.java", "templates/member/find-id.html", "templates/member/password-reset.html"],
    "인증번호 발급·확인·1회 사용과 계정 복구의 전체 흐름을 추적한다.",
    [
        page_head(1,"아이디 찾기","이름과 이메일을 확인한 뒤 마스킹 ID 표시") + [
            P("메일 요청","H2"), B("RecoveryController가 이름·이메일과 일치하는 ACTIVE 회원이 있는지 먼저 확인한다."), B("존재하면 FIND_ID 목적의 인증 코드를 발송하고 세션에 이름·정규화 이메일을 저장한다."), B("계정 존재 여부 노출을 줄이기 위해 성공·실패와 관계없이 같은 안내 문구를 반환한다."),
            P("코드 확인","H2"), B("세션의 이름·이메일이 없으면 먼저 인증번호를 요청하도록 안내한다."), B("confirmAndConsume이 코드를 검증하고 즉시 사용 처리한다."), B("MemberService.findMaskedLoginId가 아이디 일부만 보여준다."),
            P("흐름","H2"), P("입력 → 회원 일치 검사 → 이메일 발송 → 코드 확인 → 코드 소비 → 마스킹 아이디 표시", "Code")],
        page_head(2,"비밀번호 재설정","아이디와 이메일을 확인한 뒤 새 해시 저장") + [
            P("메일 요청","H2"), B("loginId + email + ACTIVE 상태가 일치하는지 검사한다."), B("PASSWORD_RESET 목적 코드를 보내고 세션에 loginId와 email을 저장한다."),
            P("변경 확정","H2"), B("새 비밀번호와 확인값, 복잡도 규칙을 확인한다."), B("인증 코드를 확인하고 1회 사용 처리한다."), B("MemberService.resetPassword가 다시 회원을 찾고 BCrypt 해시를 저장한다."),
            P("목적 분리","H2"), T([["VerificationPurpose","사용처"],["FIND_ID","아이디 찾기"],["PASSWORD_RESET","비밀번호 재설정"]],[55*mm,107*mm]),
            Spacer(1,7*mm), note("주의","코드 소비와 비밀번호 변경이 서로 다른 서비스 트랜잭션이다. DB 저장 실패까지 고려하면 두 작업을 하나의 유스케이스 트랜잭션으로 묶는 편이 안전하다.",AMBER)],
        page_head(3,"인증 코드 수명주기","EmailVerificationService의 규칙") + [
            T([["규칙","현재 값","의미"],["재요청 제한","1분","짧은 시간 내 반복 발송 방지"],["유효 시간","5분","만료된 코드 거절"],["최대 시도","5회","무차별 대입 제한"],["저장 방식","PasswordEncoder 해시","DB에 원문 코드 저장 안 함"],["사용 횟수","1회","usedAt 설정 후 재사용 차단"]],[40*mm,30*mm,92*mm]),
            Spacer(1,8*mm), P("메일 발송 모드","H2"), B("compick.mail.mode=log이면 실제 발송 대신 애플리케이션 로그에 코드를 남긴다."), B("smtp이면 JavaMailSender로 HTML 인증 메일을 발송한다."), B("SMTP 계정은 환경변수 SMTP_USERNAME, SMTP_PASSWORD를 사용한다."),
            note("운영 설정","운영에서는 log 모드를 비활성화하고 인증 코드가 로그에 평문으로 남지 않도록 해야 한다.",RED)],
    ], ["FIND_ID", "PASSWORD_RESET", "5분", "confirmAndConsume"]
))

# 04 mypage
docs.append(build(
    "04_마이페이지_회원정보_탈퇴_코드설명.pdf", "04 마이페이지·회원정보·탈퇴", "로그인 회원의 정보 조회와 계정 상태 변경",
    ["member/controller/MemberController.java", "member/service/MemberService.java", "member/dto/ProfileForm.java", "member/dto/PasswordForm.java", "member/entity/Member.java", "templates/member/mypage.html", "templates/member/profile.html"],
    "회원정보 조회·수정, 비밀번호 변경, 회원 탈퇴 코드를 이해한다.",
    [
        page_head(1,"마이페이지 조회","현재 인증 사용자로 회원 데이터를 찾는 과정") + [
            P("GET /member/mypage","H2"), B("Authentication.getName()에서 현재 loginId를 얻는다."), B("MemberService.findActiveByLoginId로 ACTIVE 회원만 조회한다."), B("AddressService.findAll로 같은 회원의 배송지 목록을 함께 전달한다."),
            P("화면 모델","H2"), T([["이름","값","사용처"],["member","Member 엔티티","이름·아이디·이메일 요약"],["addresses","배송지 목록","최근 등록 배송지 표시"]],[42*mm,55*mm,65*mm]),
            Spacer(1,8*mm), note("인가","/member/mypage는 permitAll 대상이 아니므로 로그인하지 않은 사용자는 Security가 로그인 화면으로 보낸다.",GREEN)],
        page_head(2,"회원정보와 비밀번호","변경 가능한 값과 검증") + [
            P("회원정보 수정","H2"), B("ProfileForm에는 name과 phone만 존재한다."), B("아이디와 이메일은 수정 화면에서 제거되어 변경되지 않는다."), B("Member.updateProfile이 이름과 전화번호만 반영한다."),
            P("비밀번호 변경","H2"), B("현재 비밀번호를 PasswordEncoder.matches로 비교한다."), B("새 비밀번호와 확인값이 같은지 검사한다."), B("PasswordForm의 복잡도 규칙을 통과한 새 비밀번호를 BCrypt로 다시 저장한다."),
            P("엔티티 변경 메서드","H2"), P("updateProfile(name, phone)<br/>changePassword(passwordHash)<br/>setLoginCredentials(loginId, passwordHash)<br/>withdraw()", "Code"),
            note("설계 의미","아이디·이메일 불변 정책이 DTO와 엔티티 메서드 양쪽에 반영되어 화면 조작만으로 변경할 수 없다.",BLUE)],
        page_head(3,"회원 탈퇴","물리 삭제 대신 상태 전환") + [
            P("POST /member/withdraw","H2"), B("사용자가 현재 비밀번호를 다시 입력한다."), B("MemberService.withdraw가 비밀번호를 확인한다."), B("Member.withdraw가 status를 WITHDRAWN으로 변경한다."), B("세션을 무효화하고 로그인 화면으로 이동한다."),
            P("왜 상태 전환인가","H2"), B("주문이나 결제 데이터가 추가될 때 회원 참조를 유지하기 쉽다."), B("MemberUserDetailsService가 WITHDRAWN 회원을 disabled 처리해 재로그인을 막는다."),
            P("Google 회원","H2"), B("최초 가입 시 설정한 로컬 비밀번호를 탈퇴 확인에 사용한다."),
            note("운영 정책","처리방침의 보관 기간과 실제 탈퇴 데이터 파기·익명화 절차를 일치시켜야 한다.",AMBER)],
    ], ["ProfileForm", "WITHDRAWN", "updateProfile", "PasswordEncoder.matches"]
))

# 05 address
docs.append(build(
    "05_배송지관리_코드설명.pdf", "05 배송지 관리", "회원 소유권과 기본 배송지 규칙",
    ["member/controller/AddressController.java", "member/service/AddressService.java", "member/dto/AddressForm.java", "member/entity/Address.java", "member/repository/AddressRepository.java", "templates/member/address-list.html", "templates/member/address-form.html"],
    "배송지 CRUD와 기본 배송지 1개 유지 로직을 이해한다.",
    [
        page_head(1,"CRUD 요청 흐름","목록·등록·수정·삭제") + [
            T([["기능","HTTP","Controller → Service"],["목록","GET /member/addresses","findAll(loginId)"],["등록 화면","GET /member/addresses/new","빈 AddressForm"],["수정 화면","GET /{id}/edit","getForm(loginId, id)"],["저장","POST /member/addresses 또는 /{id}","save(loginId, id, form)"],["삭제","POST /{id}/delete","delete(loginId, id)"]],[29*mm,72*mm,61*mm]),
            Spacer(1,8*mm), P("AddressForm","H2"), B("배송지명, 수령인, 전화번호, 우편번호, 기본주소, 상세주소, 기본 배송지 여부를 전달한다."), B("전화번호·필수값·길이는 Bean Validation으로 확인한다."),
            note("화면 재사용","등록과 수정은 같은 address-form.html을 사용하며 addressId 유무로 저장 경로가 달라진다.",BLUE)],
        page_head(2,"소유권 확인","다른 회원의 배송지를 막는 핵심 조건") + [
            P("findOwned","H2"), P("Long memberId = memberService.findActiveByLoginId(loginId).getId();<br/>addressRepository.findByIdAndMemberId(addressId, memberId)", "Code"),
            B("주소 ID만 조회하지 않고 현재 회원 ID를 함께 조건으로 사용한다."), B("다른 회원의 addressId를 URL에 넣어도 결과가 없으므로 수정·삭제할 수 없다."), B("수정 화면, 저장, 삭제가 모두 findOwned를 사용한다."),
            P("목록 정렬","H2"), B("findAllByMemberIdOrderByDefaultYnDescIdDesc로 기본 배송지가 먼저 나온다."), B("그다음 최근 생성된 ID 순으로 정렬한다."),
            note("보안 장점","화면에서 버튼을 숨기는 데 그치지 않고 Service 조회 조건에서 소유권을 강제한다.",GREEN)],
        page_head(3,"기본 배송지 규칙","항상 최대 1개를 유지하는 방법") + [
            P("등록·수정","H2"), B("기본 배송지로 저장하면 같은 회원의 나머지 주소를 clearDefault 처리한다."), B("첫 배송지는 사용자가 선택하지 않아도 자동으로 기본 배송지가 된다."),
            P("삭제","H2"), B("삭제 대상이 기본 배송지였는지 기억한다."), B("삭제 후 남은 주소 중 첫 번째를 makeDefault 처리한다."),
            P("Address 표현","H2"), P("defaultYn = isDefault ? \"Y\" : \"N\";<br/>isDefault() → \"Y\".equals(defaultYn)<br/>clearDefault() / makeDefault()", "Code"),
            note("확장 시","동시 저장 요청이 많아지면 서비스 로직만으로 기본 배송지 1개를 완전히 보장하기 어렵다. 잠금 또는 DB 수준 제약을 검토한다.",AMBER)],
    ], ["findByIdAndMemberId", "clearDefault", "makeDefault", "기본 배송지"]
))

# 06 security data tests
docs.append(build(
    "06_보안_DB_테스트_코드설명.pdf", "06 보안·DB·테스트", "설정 파일과 데이터 안전성, 자동 검증",
    ["common/config/SecurityConfig.java", "common/config/PasswordConfig.java", "main/resources/application.properties", "member/entity/*.java", "test/CompickApplicationTests.java", "test/member/MemberServiceIntegrationTests.java"],
    "접근 권한, DB 설정, 테이블 관계, 테스트가 보장하는 범위를 이해한다.",
    [
        page_head(1,"SecurityConfig","공개 경로와 인증 필요 경로") + [
            P("permitAll","H2"), B("메인, 개인정보 처리방침, 로그인·회원가입, 아이디 중복 확인, OAuth 콜백, 계정 복구, CSS는 비로그인 접근이 가능하다."),
            P("authenticated","H2"), B("그 외 요청은 기본적으로 로그인이 필요하다."), B("마이페이지, 프로필, 비밀번호 변경, 탈퇴, 배송지가 여기에 포함된다."),
            P("보안 기능","H2"), T([["기능","구현"],["비밀번호","BCryptPasswordEncoder"],["로그인 유지","remember-me"],["로그아웃","세션 무효화 + 쿠키 삭제"],["CSRF","Spring Security 기본 보호 유지"],["Google","조건부 oauth2Login + OIDC 사용자 서비스"]],[46*mm,116*mm]),
            Spacer(1,7*mm), note("원칙","새로운 공개 API를 추가할 때만 permitAll에 넣고, 회원 데이터 API는 기본 authenticated 정책을 유지한다.",GREEN)],
        page_head(2,"DB와 엔티티 관계","Oracle에서 관리되는 회원 데이터") + [
            T([["엔티티","관계","핵심 제약"],["Member","기준 회원","loginId unique, email unique"],["SocialAccount","ManyToOne Member","provider + providerUserId unique"],["Address","ManyToOne Member","회원별 배송지"],["EmailVerification","독립 인증 기록","email + purpose로 최신 기록 조회"]],[38*mm,54*mm,70*mm]),
            Spacer(1,8*mm), P("현재 application.properties","H2"), P("spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/ORCL<br/>spring.jpa.hibernate.ddl-auto=create-drop<br/>spring.jpa.show-sql=true", "Code"),
            note("데이터 손실 위험","create-drop은 앱 시작·종료 과정에서 테이블을 재생성·삭제한다. 테스트에는 편하지만 데이터를 보존하는 환경에서는 update, validate 또는 Flyway 마이그레이션으로 분리해야 한다.",RED)],
        page_head(3,"테스트가 확인하는 것","현재 9개 전체 통과") + [
            T([["테스트","검증 내용"],["contextLoads","Spring 설정과 Bean 기동"],["회원 통합 흐름","가입·프로필·비밀번호·탈퇴"],["중복 검사","아이디·이메일 중복 거절"],["배송지","기본 배송지 1개 유지"],["화면","회원 페이지·개인정보 처리방침 렌더링"],["자동 로그인","가입 직후 인증 세션"],["메일 인증","목적 구분과 1회 사용"],["Google","신규 생성·재사용·기존 이메일 연결"]],[48*mm,114*mm]),
            Spacer(1,8*mm), P("추가 권장","H2"), B("Google 최초 설정 화면 리다이렉트와 완료 후 재로그인"), B("타 회원 배송지 접근 차단"), B("이메일 대소문자 중복"), B("복구 과정의 DB 실패 롤백"),
            note("테스트 DB","테스트는 H2의 Oracle 호환 모드와 create-drop을 사용해 실제 Oracle 데이터를 건드리지 않는다.",BLUE)],
    ], ["permitAll", "create-drop", "9개", "BCryptPasswordEncoder"]
))

for path, pages in docs:
    print(f"{path.name}|{pages}")
