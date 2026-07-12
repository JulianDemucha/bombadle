import '../style/footer.css';

function Footer() {
    return (<>
        <footer className="bombadle-footer">
            <span style={{marginTop: '6rem'}}>Bombadle - 2026</span>
            <a href="/privacy_policy.html" target="_blank" rel="noopener noreferrer">Polityka Prywatności</a>
            <a href="/regulamin.html" target="_blank" rel="noopener noreferrer">Regulamin strony</a>
            <span style={{fontSize: '11px', opacity: '0.8', lineHeight: '1.5'}}>
                Ta strona jest nieoficjalną fanowską witryną poświęconą uniwersum Kapitana Bomby.
                Nie jest powiązana, sponsorowana ani zatwierdzona przez twórców, producentów lub nadawców serialu.
                Wszystkie prawa do postaci, cytatów, grafik i nazw należą do ich prawowitych właścicieli.
                Materiały wykorzystano wyłącznie w celach fanowskich, informacyjnych i rozrywkowych, bez zamiaru uzyskania korzyści majątkowych.
            </span>
            <br></br>
        </footer>
    </>)
}

export default Footer;