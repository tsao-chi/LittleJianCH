package littlejian.examples.pie

def gensym(x: String): String = x + "%" + java.util.concurrent.ThreadLocalRandom.current.nextInt.toHexString