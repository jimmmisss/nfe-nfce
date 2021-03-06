package br.com.notafiscal.nfenfce;

import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.impressao.service.ImpressaoService;
import br.com.swconsultoria.impressao.util.ImpressaoUtil;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.DocumentoEnum;
import br.com.swconsultoria.nfe.schema_4.enviNFe.*;
import br.com.swconsultoria.nfe.util.ChaveUtil;
import br.com.swconsultoria.nfe.util.ConstantesUtil;
import br.com.swconsultoria.nfe.util.RetornoUtil;
import br.com.swconsultoria.nfe.util.XmlNfeUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum.HOMOLOGACAO;
import static br.com.swconsultoria.nfe.dom.enuns.EstadosEnum.SC;
import static br.com.swconsultoria.nfe.dom.enuns.StatusEnum.LOTE_EM_PROCESSAMENTO;

@Slf4j
public class Teste {

    public static void main(String[] args) {

        try {
            emiteNef();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static ChaveUtil chaveUtil;
    private static ConfiguracoesNfe configuracoesNfe;
    private static String cnpj;
    private static String modelo;
    private static String tipoEmissao;
    private static String cnf;
    private static int serie;
    private static int numeroNF;
    private static LocalDateTime dataEmissao;

    private static void emiteNef() throws Exception {
        cnpj = "32527739000142";
        modelo = "55";
        serie = 1;
        numeroNF = 92723;
        tipoEmissao = "1";
        cnf = String.format("%08d", new Random().nextInt(99999999));
        dataEmissao = LocalDateTime.now();

        String xmlFinal;

        // Inicia configura????es (certificado digital)
        criaConfiguracoes();

        // Inicia a chave da nota fiscal
        montaChaveNFe(configuracoesNfe);

        // Cria dados da nota fical
        var enviNFe = criaEnviNFe();

        //Efetua assinatura e valida????o
        enviNFe = Nfe.montaNfe(configuracoesNfe, enviNFe, true);

        //Envio da nota fical eletronica
        var retorno = Nfe.enviarNfe(configuracoesNfe, enviNFe, DocumentoEnum.NFE);

        //Faz verifica????o se o retorno ?? assincrono e consulta o recibo
        if (RetornoUtil.isRetornoAssincrono(retorno)) {
            var tRetConsReciNFe = verificaEnvioAssincrono(retorno);
            xmlFinal = XmlNfeUtil.criaNfeProc(enviNFe, tRetConsReciNFe.getProtNFe().get(0));
            RetornoUtil.validaAssincrono(tRetConsReciNFe);
            log.info("STATUS: " + tRetConsReciNFe.getProtNFe().get(0).getInfProt().getCStat());
            log.info("PROTOCOLO: " + tRetConsReciNFe.getProtNFe().get(0).getInfProt().getNProt());
            log.info("XML FINAL: " + xmlFinal);
        } else {
            RetornoUtil.validaSincrono(retorno);
            xmlFinal = XmlNfeUtil.criaNfeProc(enviNFe, retorno.getProtNFe());
            log.info("STATUS: " + retorno.getProtNFe().getInfProt().getCStat());
            log.info("PROTOCOLO: " + retorno.getProtNFe().getInfProt().getNProt());
            log.info("XML FINAL: " + xmlFinal);
        }

        getEfetuarImpressaoNFe(xmlFinal);
    }

    private static void getEfetuarImpressaoNFe(String xmlFinal) throws JRException, ParserConfigurationException, IOException, SAXException {
        var impressao = ImpressaoUtil.impressaoPadraoNFe(xmlFinal);
        ImpressaoService.impressaoPdfArquivo(impressao, "/home/wesley/Documents/wesley/nfe-teste.pdf");
    }

    private static br.com.swconsultoria.nfe.schema_4.retConsReciNFe.TRetConsReciNFe verificaEnvioAssincrono(TRetEnviNFe retorno) throws Exception {
        var recibo = retorno.getInfRec().getNRec();
        int tentativa = 1;
        br.com.swconsultoria.nfe.schema_4.retConsReciNFe.TRetConsReciNFe retornoConsulta = null;
        while (true) {
            retornoConsulta = Nfe.consultaRecibo(configuracoesNfe, recibo, DocumentoEnum.NFE);
            if (retornoConsulta.getCStat().equals(LOTE_EM_PROCESSAMENTO.getCodigo())) {
                Thread.sleep(1000);
                tentativa++;
                if (tentativa > 10) {
                    // Salvar o recibo no banco de dados e consultar posteriormente
                    throw new Exception("Lote em processamento. Agarde um tempo e tente novamente.");
                }
            } else {
                break;
            }
        }
        return retornoConsulta;
    }

    private static void montaChaveNFe(ConfiguracoesNfe configuracoesNfe) {
        chaveUtil = new ChaveUtil(
                configuracoesNfe.getEstado(),
                cnpj,
                modelo,
                serie,
                numeroNF,
                tipoEmissao,
                cnf,
                dataEmissao);
    }

    private static TEnviNFe criaEnviNFe() {
        var enviNFe = new TEnviNFe();
        enviNFe.setVersao(ConstantesUtil.VERSAO.NFE);
        enviNFe.setIdLote("1");
        enviNFe.setIndSinc("1");

        var nfe = new TNFe();
        nfe.setInfNFe(montaInfNFe());

        enviNFe.getNFe().add(nfe);

        return enviNFe;
    }

    private static TNFe.InfNFe montaInfNFe() {
        var infNFe = new TNFe.InfNFe();

        infNFe.setId(chaveUtil.getChaveNF());
        infNFe.setVersao(ConstantesUtil.VERSAO.NFE);
        infNFe.setIde(montaIde());
        infNFe.setEmit(montaEmitente());
        infNFe.setDest(montaDestinatario());
        infNFe.getDet().addAll(montaDet());
        infNFe.setTransp(montaTransportadora());
        infNFe.setPag(montaPagamento());
        //infNFe.setInfAdic();
        infNFe.setInfRespTec(montaRespTecnico());
        infNFe.setTotal(montaTotal());

        return infNFe;
    }

    private static TNFe.InfNFe.Total montaTotal() {
        var total = new TNFe.InfNFe.Total();
        var icmsTot = new TNFe.InfNFe.Total.ICMSTot();
        icmsTot.setVBC("10.00");
        icmsTot.setVICMS("1.00");
        icmsTot.setVICMSDeson("0.00");
        icmsTot.setVFCP("0.00");
        icmsTot.setVBCST("0.00");
        icmsTot.setVST("0.00");
        icmsTot.setVFCPST("0.00");
        icmsTot.setVFCPSTRet("0.00");
        icmsTot.setVProd("10.00");
        icmsTot.setVFrete("0.00");
        icmsTot.setVSeg("0.00");
        icmsTot.setVDesc("0.00");
        icmsTot.setVII("0.00");
        icmsTot.setVIPI("0.00");
        icmsTot.setVIPIDevol("0.00");
        icmsTot.setVPIS("0.17");
        icmsTot.setVCOFINS("0.76");
        icmsTot.setVOutro("0.00");
        icmsTot.setVNF("10.00");

        total.setICMSTot(icmsTot);

        return total;
    }

    private static TInfRespTec montaRespTecnico() {
        var respTec = new TInfRespTec();
        respTec.setCNPJ("32330160000195");
        respTec.setXContato("Wesley Pereira");
        respTec.setFone("contato@legalizzr.com.br");
        respTec.setEmail("48999437427");

        return respTec;
    }

    private static TNFe.InfNFe.Pag montaPagamento() {
        var pag = new TNFe.InfNFe.Pag();
        var detPag = new TNFe.InfNFe.Pag.DetPag();
        detPag.setTPag("01");
        detPag.setVPag("10.00");

        pag.getDetPag().add(detPag);

        return pag;
    }

    private static TNFe.InfNFe.Transp montaTransportadora() {
        var transp = new TNFe.InfNFe.Transp();
        transp.setModFrete("9");

        return transp;
    }

    private static List<TNFe.InfNFe.Det> montaDet() {
        var det = new TNFe.InfNFe.Det();

        det.setNItem("1");
        det.setProd(montaProduto());
        det.setImposto(montaImposto());

        return Collections.singletonList(det);
    }

    private static TNFe.InfNFe.Det.Imposto montaImposto() {
        var imposto = new TNFe.InfNFe.Det.Imposto();

        criaImpostoIcms(imposto);
        criaImpostoPis(imposto);
        criaImpostoCofins(imposto);

        return imposto;
    }

    private static void criaImpostoPis(TNFe.InfNFe.Det.Imposto imposto) {
        var pis = new TNFe.InfNFe.Det.Imposto.PIS();
        var pisAliq = new TNFe.InfNFe.Det.Imposto.PIS.PISAliq();
        pisAliq.setCST("01");
        pisAliq.setVBC("10.00");
        pisAliq.setPPIS("1.65");
        pisAliq.setVPIS("0.17");

        pis.setPISAliq(pisAliq);

        imposto.getContent().add(new ObjectFactory().createTNFeInfNFeDetImpostoPIS(pis));
    }

    private static void criaImpostoCofins(TNFe.InfNFe.Det.Imposto imposto) {
        var cofins = new TNFe.InfNFe.Det.Imposto.COFINS();
        var cofinsAliq = new TNFe.InfNFe.Det.Imposto.COFINS.COFINSAliq();
        cofinsAliq.setCST("01");
        cofinsAliq.setVBC("10.00");
        cofinsAliq.setPCOFINS("7.60");
        cofinsAliq.setVCOFINS("0.76");

        cofins.setCOFINSAliq(cofinsAliq);

        imposto.getContent().add(new ObjectFactory().createTNFeInfNFeDetImpostoCOFINS(cofins));
    }

    private static void criaImpostoIcms(TNFe.InfNFe.Det.Imposto imposto) {
        var icms = new TNFe.InfNFe.Det.Imposto.ICMS();
        var icms00 = new TNFe.InfNFe.Det.Imposto.ICMS.ICMS00();
        icms00.setOrig("0");
        icms00.setModBC("0");
        icms00.setCST("00");
        icms00.setVBC("10.00");
        icms00.setPICMS("10");
        icms00.setVICMS("1.00");
        icms.setICMS00(icms00);
        imposto.getContent().add(new ObjectFactory().createTNFeInfNFeDetImpostoICMS(icms));
    }

    private static TNFe.InfNFe.Det.Prod montaProduto() {
        var produto = new TNFe.InfNFe.Det.Prod();

        produto.setCProd("123");
        produto.setCEAN("SEM GTIN");
        produto.setXProd("NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGA????O - SEM VALOR FISCAL");
        produto.setNCM("27101932");
        produto.setCEST("0600500");
        produto.setIndEscala("S");
        produto.setCFOP("6405");
        produto.setUCom("UN");
        produto.setQCom("1");
        produto.setVUnCom("10");
        produto.setVProd("10.00");
        produto.setCEANTrib("SEM GTIN");
        produto.setUTrib("UN");
        produto.setQTrib("1");
        produto.setVUnTrib("10");
        produto.setIndTot("1");

        return produto;
    }

    private static TNFe.InfNFe.Dest montaDestinatario() {
        var dest = new TNFe.InfNFe.Dest();
        dest.setXNome("NF-E EMITIDA EM AMBIENTE DE HOMOLOGA????O - SEM VALOR FISCAL");
        dest.setCNPJ("10732644000128");
        dest.setIE("104519304");
        dest.setIndIEDest("1");

        var enderecoEmitente = new TEndereco();
        enderecoEmitente.setXLgr("Rua teste");
        enderecoEmitente.setNro("0");
        enderecoEmitente.setXCpl("Qd 1 lote 1");
        enderecoEmitente.setXBairro("Centro");
        enderecoEmitente.setCMun("52119753");
        enderecoEmitente.setXMun("SANTO AMARO");
        enderecoEmitente.setUF(TUf.GO);
        enderecoEmitente.setCEP("88131626");

        dest.setEnderDest(enderecoEmitente);
        return dest;
    }

    private static TNFe.InfNFe.Emit montaEmitente() {
        var emit = new TNFe.InfNFe.Emit();
        emit.setXNome("Nome da Empresa");
        emit.setCNPJ(cnpj);
        emit.setIE("104519304");
        emit.setCRT("3");

        var enderecoEmitente = new TEnderEmi();
        enderecoEmitente.setXLgr("Rua teste");
        enderecoEmitente.setNro("0");
        enderecoEmitente.setXCpl("Qd 1 lote 1");
        enderecoEmitente.setXBairro("Centro");
        enderecoEmitente.setCMun("52119753");
        enderecoEmitente.setXMun("SANTO AMARO");
        enderecoEmitente.setUF(TUfEmi.valueOf(configuracoesNfe.getEstado().toString()));
        enderecoEmitente.setCEP("88131626");

        emit.setEnderEmit(enderecoEmitente);

        return emit;
    }

    private static TNFe.InfNFe.Ide montaIde() {
        var ide = new TNFe.InfNFe.Ide();
        ide.setCNF(configuracoesNfe.getEstado().getCodigoUF());
        ide.setCNF(cnf);
        ide.setNatOp("Venda NFe");
        ide.setMod(modelo);
        ide.setSerie(String.valueOf(serie));
        ide.setNNF(String.valueOf(numeroNF));
        ide.setDhEmi(XmlNfeUtil.dataNfe(dataEmissao));
        ide.setTpNF("1");
        ide.setIdDest("2");
        ide.setCMunFG("5219753");
        ide.setTpImp("1");
        ide.setTpEmis(tipoEmissao);
        ide.setCDV(chaveUtil.getDigitoVerificador());
        ide.setTpAmb(configuracoesNfe.getAmbiente().getCodigo());
        ide.setFinNFe("1");
        ide.setIndFinal("1");
        ide.setIndPres("1");
        ide.setProcEmi("0");
        ide.setVerProc("1.0.0");

        return ide;
    }

    private static void criaConfiguracoes() throws CertificadoException, FileNotFoundException {
        var certificado = CertificadoService.certificadoPfx("/home/wesley/Documents/wesley/certificado", "123456");
        configuracoesNfe = ConfiguracoesNfe.criarConfiguracoes(SC, HOMOLOGACAO, certificado, "/home/wesley/Documents/wesley/schemas");
    }
}