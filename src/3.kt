import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

const val TAMANHO_MAPA = 20
const val PROVISOES_INICIAIS = 40
const val TRIPULACAO_INICIAL = 5

enum class Clima(val descricao: String, val multiplicador: Double) {
    CALMARIA("Mar calmo", 1.0),
    VENTO_FORTE("Vento forte", 1.5),
    TEMPESTADE("Tempestade", 2.0)
}

class Navio(
    var x: Int = 0,
    var y: Int = 0,
    var provisoes: Int = PROVISOES_INICIAIS,
    var tripulacao: Int = TRIPULACAO_INICIAL
)

// ---------- Entrada de dados (com null safety) ----------

fun lerTexto(mensagem: String): String? {
    print(mensagem)
    return readLine()?.trim() // null se a entrada acabar (EOF)
}

fun lerCoordenada(nome: String): Int? {
    val valor = lerTexto("  $nome (0-$TAMANHO_MAPA): ")?.toIntOrNull()
    return if (valor != null && valor in 0..TAMANHO_MAPA) valor else null
}

// ---------- Logica do jogo ----------

fun calcularDistancia(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    val dx = (x2 - x1).toDouble()
    val dy = (y2 - y1).toDouble()
    return sqrt(dx * dx + dy * dy)
}

fun definirClima(vento: Int): Clima = when {
    vento >= 8 -> Clima.TEMPESTADE
    vento >= 5 -> Clima.VENTO_FORTE
    else -> Clima.CALMARIA
}

fun calcularCusto(distancia: Double, clima: Clima): Int =
    ceil(distancia * clima.multiplicador / 2).toInt()

fun gerarMapa(tx: Int, ty: Int): List<String?> {
    val quadrante = when {
        tx >= 10 && ty >= 10 -> "nordeste"
        tx >= 10 && ty < 10 -> "sudeste"
        tx < 10 && ty >= 10 -> "noroeste"
        else -> "sudoeste"
    }
    val paridade = if (tx % 2 == 0) "par" else "impar"

    val pecas = listOf(
        "O X marca a coluna X = $tx",
        "O X marca a linha Y = $ty",
        "A soma das coordenadas do tesouro e ${tx + ty}",
        "A coordenada X e um numero $paridade",
        "O tesouro esta no quadrante $quadrante"
    )
    // 40% de chance de cada pedaco do mapa estar rasgado (null)
    return pecas.map { if (Random.nextInt(100) < 40) null else it }
}

fun mostrarMapa(mapa: List<String?>) {
    println("\nMAPA DO TESOURO")
    for ((i, peca) in mapa.withIndex()) {
        println("  ${i + 1}. ${peca ?: "[trecho rasgado]"}")
    }
    val legiveis = mapa.filterNotNull().size
    println("  ($legiveis de ${mapa.size} trechos legiveis)")
}

fun mostrarStatus(navio: Navio, rodada: Int, clima: Clima) {
    println("\n==============================")
    println("Dia $rodada | Clima: ${clima.descricao}")
    println("Posicao: (${navio.x}, ${navio.y})")
    println("Provisoes: ${navio.provisoes} | Tripulacao: ${navio.tripulacao}")
    println("==============================")
}

fun dicaDeProximidade(distancia: Double): String = when {
    distancia < 5 -> "Muito quente! O tesouro esta perto!"
    distancia < 10 -> "Morno... voce esta no caminho."
    else -> "Frio... o tesouro esta longe."
}

// Retorna true se o jogador encontrou o tesouro
fun navegar(navio: Navio, clima: Clima, tx: Int, ty: Int): Boolean {
    println("\nPara onde vamos, capitao?")
    val destX = lerCoordenada("X")
    val destY = lerCoordenada("Y")

    if (destX == null || destY == null) {
        println("Coordenada invalida! A tripulacao nao entendeu a ordem.")
        return false
    }

    val distancia = calcularDistancia(navio.x, navio.y, destX, destY)
    val custo = calcularCusto(distancia, clima)

    if (custo > navio.provisoes) {
        println("Provisoes insuficientes! Essa viagem custa $custo e voce tem ${navio.provisoes}.")
        return false
    }

    navio.provisoes -= custo
    navio.x = destX
    navio.y = destY
    println("Viajou %.1f milhas e gastou $custo provisoes.".format(distancia))

    if (clima == Clima.TEMPESTADE && navio.provisoes < 15) {
        navio.tripulacao--
        println("A tempestade levou um marujo ao mar! Restam ${navio.tripulacao}.")
    }

    val distanciaTesouro = calcularDistancia(destX, destY, tx, ty)
    if (distanciaTesouro <= 1.0) return true

    println("Bussola: %.1f milhas ate o X. ${dicaDeProximidade(distanciaTesouro)}".format(distanciaTesouro))
    return false
}

fun mostrarMenu() {
    println("\n1 - Navegar ate coordenadas")
    println("2 - Ler o mapa")
    println("3 - Desistir")
}

// ---------- Programa principal ----------

fun main() {
    println("CACA AO TESOURO PIRATA")
    val nome = lerTexto("Qual o seu nome, capitao? ")
    val capitao = if (nome.isNullOrBlank()) "Capitao Sem Nome" else nome
    println("Bem-vindo, $capitao! O mapa vai de 0 a $TAMANHO_MAPA em X e Y.")

    val tesouroX = Random.nextInt(0, TAMANHO_MAPA + 1)
    val tesouroY = Random.nextInt(0, TAMANHO_MAPA + 1)
    val mapa = gerarMapa(tesouroX, tesouroY)
    val navio = Navio()

    var rodada = 1
    var venceu = false

    loop@ while (!venceu && navio.provisoes > 0 && navio.tripulacao > 0) {
        val clima = definirClima(Random.nextInt(0, 11))
        mostrarStatus(navio, rodada, clima)
        mostrarMenu()

        // readLine() == null significa fim da entrada: encerra o jogo
        val opcao = lerTexto("Escolha: ")
        if (opcao == null) {
            break@loop
        }

        when (opcao) {
            "1" -> {
                venceu = navegar(navio, clima, tesouroX, tesouroY)
                rodada++
            }
            "2" -> mostrarMapa(mapa)
            "3" -> {
                println("Voce desistiu da aventura...")
                break@loop
            }
            "" -> println("Voce nao digitou nada! Escolha 1, 2 ou 3.")
            else -> println("Opcao '$opcao' invalida! Escolha 1, 2 ou 3.")
        }
    }

    println()
    if (venceu) {
        println("PARABENS, $capitao! Voce encontrou o tesouro em ($tesouroX, $tesouroY) no dia ${rodada - 1}!")
    } else if (navio.provisoes <= 0 || navio.tripulacao <= 0) {
        println("Fim de jogo, $capitao. O tesouro estava em ($tesouroX, $tesouroY).")
    } else {
        println("O tesouro estava em ($tesouroX, $tesouroY). Ate a proxima!")
    }
}