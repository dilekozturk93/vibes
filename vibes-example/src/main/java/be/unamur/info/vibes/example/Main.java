package be.unamur.info.vibes.example;

import be.vibes.dsl.io.Dot;
import be.vibes.ts.FeaturedTransitionSystem;

/**
 *
 * @author Xavier Devroey - xavier.devroey@unamur.be
 */
public class Main {

    public static void main(String[] args) throws Exception {
        FeaturedTransitionSystem svm = new SodaVendingMachineModel().getTransitionSystem();
        System.out.println(Dot.format(svm));
    }

}
