package br.com.notafiscal.nfenfce;

import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.*;
import br.com.swconsultoria.nfe.util.ChaveUtil;
import br.com.swconsultoria.nfe.util.XmlNfeUtil;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum.HOMOLOGACAO;
import static br.com.swconsultoria.nfe.dom.enuns.EstadosEnum.SC;
import static br.com.swconsultoria.nfe.util.ConstantesUtil.VERSAO.NFE;

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

        criaConfiguracoes();
        montaChaveNFe(configuracoesNfe);

        var enviNFe = criaEnviNFe();
        enviNFe = Nfe.montaNfe(configuracoesNfe, enviNFe, true);
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
        var nfe = new TNFe();

        montaInfNFe();

        //nfe.setInfNFe();

        enviNFe.getNFe().add(nfe);

        return enviNFe;
    }

    private static TNFe.InfNFe montaInfNFe() {
        var infNFe = new TNFe.InfNFe();

        infNFe.setId(chaveUtil.getChaveNF());
        infNFe.setVersao(NFE);
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
        icmsTot.setVBC("0.00");
        icmsTot.setVICMS("0.00");
        icmsTot.setVICMSDeson("0.00");
        icmsTot.setVFCP("0.00");
        icmsTot.setVBCST("0.00");
        icmsTot.setVST("0.00");
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

        //imposto.getContent().add(new ObjectFactory().createTNFeInfNFeDetImpostoPIS(cofins));
    }

    private static void criaImpostoIcms(TNFe.InfNFe.Det.Imposto imposto) {
        var icms = new TNFe.InfNFe.Det.Imposto.ICMS();
        var icms60 = new TNFe.InfNFe.Det.Imposto.ICMS.ICMS60();
        icms60.setOrig("0");
        icms60.setCST("60");
        icms60.setVBCSTRet("0.00");
        icms60.setPST("0.00");

        icms.setICMS60(icms60);
        imposto.getContent().add(new ObjectFactory().createTNFeInfNFeDetImpostoICMS(icms));
    }

    private static TNFe.InfNFe.Det.Prod montaProduto() {
        var produto = new TNFe.InfNFe.Det.Prod();

        produto.setCProd("123");
        produto.setCEAN("SEM GTIN");
        produto.setXProd("NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGAÇÃO - SEM VALOR FISCAL");
        produto.setNCM("27101932");
        produto.setCEST("0600500");
        produto.setIndEscala("S");
        produto.setCFOP("5405");
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
        dest.setXNome("Nome da Empresa");
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


























