abstract class DiscountPolicy {
    abstract double discount(double subtotal);
}
class FlatDiscount extends DiscountPolicy {
    private final double amount;
    FlatDiscount(double amount){this.amount=amount;}
    @Override double discount(double subtotal){return Math.min(amount,subtotal);}
}
class PercentageDiscount extends DiscountPolicy {
    private final double percent;
    PercentageDiscount(double percent){this.percent=percent;}
    @Override double discount(double subtotal){return subtotal*percent/100.0;}
}